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

package netconfig.io.json
import core_extensions.MMLogging
import netconfig.Link
import netconfig.io.files.SerializedNetwork
import network.gen.NetworkBuilder
import netconfig.io.Serializer
import netconfig.storage.LinkIDRepr
import network.gen.GenericLink

/**
 * A collection of utilities to materialize a network from a JSON representation on disk, using generic links.
 *
 * As long as a network is represented using a GenericLinkRepresentation, you can safely materialize this network
 * using the methods in this object.
 */
object NetworkUtils extends MMLogging {
  /**
   * Materializes links from a network ID and a network type.
   */
  def getLinks(network_id: Int, net_typesource_name: String): Seq[Link] = {
    val fname = SerializedNetwork.fileName(network_id, net_typesource_name)
    val glrs = JSonSerializer.getGenericLinks(fname)
    val builder = new NetworkBuilder
    builder.build(glrs)
  }

  /**
   * Creates a serializer from a set of links.
   *
   * TODO(tjh) this function could be moved somewhere else.
   */
  def getSerializer(links: Seq[Link]): Serializer[Link] = {
    val map: Map[LinkIDRepr, Link] = Map.empty ++ links.map(l => {
      val linkId = l.asInstanceOf[GenericLink].idRepr
      (linkId, l)
    })
    JSonSerializer.from(map)
  }

  /**
   * Creates a serializer for a network.
   */
  def getSerializer(net_type: String, net_id: Int): Serializer[Link] = {
    getSerializer(getLinks(net_id, net_type))
  }
}