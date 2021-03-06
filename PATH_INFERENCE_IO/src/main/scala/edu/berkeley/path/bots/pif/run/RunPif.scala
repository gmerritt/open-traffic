/**
 * Copyright 2012. The Regents of the University of California (Regents).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package pif.run

import java.io.File
import org.joda.time.LocalDate
import edu.berkeley.path.bots.core.MMLogging
import edu.berkeley.path.bots.core.TimeUtils.TimeOrdering
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.io.Dates.parseDate
import edu.berkeley.path.bots.netconfig.io.Dates.parseRange
import edu.berkeley.path.bots.netconfig.io.files.PathInferenceViterbi
import edu.berkeley.path.bots.netconfig.io.files.ProbeCoordinateViterbi
import edu.berkeley.path.bots.netconfig.io.files.RawProbe
import edu.berkeley.path.bots.netconfig.io.files.SerializedNetwork
import edu.berkeley.path.bots.netconfig.io.files.TrajectoryViterbi
import edu.berkeley.path.bots.netconfig.io.json.JSonSerializer
import edu.berkeley.path.bots.netconfig.io.Serializer
import edu.berkeley.path.bots.netconfig.storage.LinkIDRepr
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.network.gen.GenericLink
import edu.berkeley.path.bots.network.gen.NetworkBuilder
import path_inference.crf.ComputingStrategy
import path_inference.manager.ProjectionHook
import path_inference.manager.ProjectionHookInterface
import path_inference.PathInferenceFilter
import path_inference.PathInferenceParameters2
import scopt.OptionParser
import edu.berkeley.path.bots.netconfig.io.Dates
import scala.actors.Futures._
import edu.berkeley.path.bots.netconfig.io.json.NetworkUtils
import path_inference.shortest_path.PathGenerator2

/**
 * Runs the path inference on some serialized data, using a generic network representation.
 *
 * It uses some default sensible parameters for best accuracy.
 *
 * mvn install
 *
 * Debugging procedure for the PIF
 *
 */
object RunPif extends MMLogging {

  def main(args: Array[String]) = {
    // All the options
    import Dates._
    var network_id: Int = -1
    var date: LocalDate = null
    var range: Option[Seq[LocalDate]] = Some(Seq.empty[LocalDate])
    var feed: String = ""
    var driver_id: String = ""
    var net_type: String = ""
    var num_threads: Int = 1
    var extended_info: Boolean = false
    var sort_time: Boolean = false
    val parser = new OptionParser("test") {
      intOpt("nid", 
          "(required, integer) - " +
          "The identifier of the network", network_id = _)
      intOpt("num-threads",
          "(optional, default to %d) the number of threads. the program will use one thread per day.".format(num_threads),
          num_threads = _)
      opt("date", "(optional) the date. The format is YYYY-MM-DD (if the date is empty, it will try to process everything " +
      		"available for this feed)",
          { s: String => { for (d <- parseDate(s)) { date = d } } })
      opt("range", "(optional) a range of dates. The format is YYYY-MM-DD:YYYY-MM-DD. " +
      		"Cannot be used with the 'date' argument. " +
      		"If both 'date' and 'range' are unspecified, the program will process everything.",
          (s: String) => for (r <- parseRange(s)) { range = Some(r) })
      opt("feed", "(required, string)" +
      		" The name of the data feed", 
      		feed = _)
      opt("net-type", "(required, string)" +
      		"The type of the network.",
      		net_type = _)
      opt("driver_id", "(optional, list of comma-separated strings) " +
      		"Runs the filter on the selected driver ids",
      		driver_id = _)
      booleanOpt("extended-info", "Adds additional (redundant) information in the output file. " +
      		"Useful for python.", extended_info = _)
      booleanOpt("resort-data", "(optional, default to %s)".format(sort_time.toString) +
      		"sort the data by timestamp before sending it to the PIF", sort_time = _)
    }
    parser.parse(args)
    if (network_id == -1) {
      parser.showUsage
      throw new IllegalArgumentException
    }
      

    val parameters = pifParameters()

    logInfo("Loading links...")
    var net = NetworkUtils.getLinks(network_id, net_type)
    val links = net.values.toIndexedSeq

    val serializer: Serializer[Link] = NetworkUtils.getSerializer(net)

    logInfo("Building projector...")
    val projection_hook: ProjectionHookInterface = ProjectionHook.create(links, parameters)

    val path_gen = PathGenerator2.getDefaultPathGenerator(parameters)

    // Can be null
    val date_range: Seq[LocalDate] = {
      if (date != null) {
        Seq(date)
      } else {
        range.getOrElse(null)
      }
    }

    val drivers_whitelist = if (driver_id.isEmpty) {
      Set.empty[String]
    } else {
      driver_id.split(",").toSet
    }

    logInfo("Number of selected dates: %s" format {if (date_range==null) "everything" else date_range.size.toString})
    logInfo("Feed:" + feed)
    logInfo("Driver whitelist: %s" format drivers_whitelist.toString)
    logInfo("Presorting data: %s" format sort_time)

    val batched_indexes = RawProbe.list(feed = feed, nid = network_id, dates = date_range)
      .zipWithIndex
      .groupBy({ case (x, i) => i % num_threads })
      .values
      .map(_.map(_._1))
      .toSeq
      
    val num_indexes = batched_indexes.map(_.size).sum
    if (num_indexes > 0) {
      logInfo("Processing %d tasks with %d threads.".format(num_indexes, num_threads))
    } else {
      logInfo("No task to process, exiting. If this is unexpected, check your parameters and your filtering options (date, range, driver-id).")
    }

    val tasks = for (findexes <- batched_indexes) yield {
      future {
        for (findex <- findexes) yield {
          runPIF(projection_hook,
            path_gen,
            serializer,
            net_type,
            parameters,
            findex,
            drivers_whitelist,
            extended_info,
            sort_time)
        }
      }
    }

    for (task <- tasks) {
      task()
    }
    logInfo("Done")
  }

  def pifParameters() = {
    val params = new PathInferenceParameters2()
    params.fillDefaultFor1MinuteOffline
    params.setReturnPoints(true)
    params.setReturnRoutes(true)
    params.setMinPathProbability(0.1)
    params.setMinProjectionProbability(0.1)
    params.setShuffleProbeCoordinateSpots(true)
    params.setComputingStrategy(ComputingStrategy.Viterbi)
    params.setPathsCacheSize(6000000) // 6M elements
    params
  }

  def runPIF(
    projector: ProjectionHookInterface,
    path_gen: PathGenerator2,
    serializer: Serializer[Link],
    net_type: String,
    parameters: PathInferenceParameters2,
    file_index: RawProbe.FileIndex,
    drivers_whitelist: Set[String],
    extended_info: Boolean,
    sort_time: Boolean): Unit = {

    val fname_in = RawProbe.fileName(file_index)
    val fname_pcs = ProbeCoordinateViterbi.fileName(feed = file_index.feed,
      nid = file_index.nid,
      date = file_index.date,
      net_type = net_type)
    val fname_pis = PathInferenceViterbi.fileName(feed = file_index.feed,
      nid = file_index.nid,
      date = file_index.date,
      net_type = net_type)
    val fname_trajs = (vid: String, traj_idx: Int) =>
      TrajectoryViterbi.fileName(file_index.feed, file_index.nid, file_index.date,
        net_type, vid, traj_idx)

    assert((new File(fname_in)).exists())

    val writer_pi = serializer.writerPathInference(fname_pis, extended_info)
    val writer_pc = serializer.writerProbeCoordinate(fname_pcs, extended_info)
    logInfo("Opening data source: %s" format fname_in)
    val data = {
      val x = serializer.readProbeCoordinates(fname_in)
      if (sort_time) {
        x.toArray.sortBy(_.time).toIterable
      } else {
        x.toIterable
      }
    }
    logInfo("Opened data source: %s" format fname_in)
    val pif = PathInferenceFilter.createManager(parameters, projector, path_gen)
    for (raw <- data) {
      val pc = raw
      if (drivers_whitelist.isEmpty || pc.id == null || drivers_whitelist.contains(pc.id)) {
        pif.addPoint(pc)
        for (out_pi <- pif.getPathInferences) {
          writer_pi.put(out_pi)
        }
        for (out_pc <- pif.getProbeCoordinates) {
          writer_pc.put(out_pc)
        }
      }
    }
    pif.finalizeManager
    for (out_pi <- pif.getPathInferences) {
      writer_pi.put(out_pi)
    }
    for (out_pc <- pif.getProbeCoordinates) {
      writer_pc.put(out_pc)
    }
    writer_pc.close()
    writer_pi.close()
    logInfo("Closed data source: %s" format fname_in)
  }
}