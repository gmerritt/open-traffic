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

package edu.berkeley.path.bots.network.arterial1
import org.junit._
import org.junit.Assert._
import com.codahale.jerkson.Json._
import edu.berkeley.path.bots.network.gen.GenericLinkRepresentation

class SerializationTest {

  // scalastyle:off
  val string = """ {"id":{"primary":170832,"secondary":1},"length":731.9842529296875,"startNodeId":{"primary":170832,"secondary":1},"endNodeId":{"primary":170832,"secondary":2},"geom":{"points":[{"lat":37.80106649202754,"lon":-122.41945229109076,"srid":4326},{"lat":37.80013726477779,"lon":-122.4192724462609,"srid":4326},{"lat":37.799477009191655,"lon":-122.41913239414086,"srid":4326},{"lat":37.79919697949971,"lon":-122.41907238761127,"srid":4326},{"lat":37.798897637564096,"lon":-122.41902251282839,"srid":4326},{"lat":37.79873734829551,"lon":-122.41899246151625,"srid":4326},{"lat":37.79855687091322,"lon":-122.41895236368893,"srid":4326},{"lat":37.798256719516914,"lon":-122.41888232915066,"srid":4326},{"lat":37.79733723563824,"lon":-122.41870243974039,"srid":4326},{"lat":37.79642720595245,"lon":-122.41852243362221,"srid":4326},{"lat":37.79631708955833,"lon":-122.41850241394846,"srid":4326},{"lat":37.79618675429787,"lon":-122.41847233658066,"srid":4326},{"lat":37.79571729303735,"lon":-122.41838245022504,"srid":4326},{"lat":37.79545728171458,"lon":-122.41833244791005,"srid":4326},{"lat":37.794556870775594,"lon":-122.41813236266997,"srid":4326},{"lat":37.79455581991619,"lon":-122.41813215733524,"srid":4326}]},"endFeature":"signal","numLanes":1,"speedLimit":11.175999641418457}
 {"id":{"primary":171720,"secondary":1},"length":145.85467529296875,"startNodeId":{"primary":171720,"secondary":1},"endNodeId":{"primary":33811,"secondary":-1},"geom":{"points":[{"lat":37.80502313209764,"lon":-122.43371843037527,"srid":4326},{"lat":37.80502258348775,"lon":-122.43371582447831,"srid":4326},{"lat":37.805032067885634,"lon":-122.43349749054934,"srid":4326},{"lat":37.80511216041358,"lon":-122.43287677345778,"srid":4326},{"lat":37.80519115555187,"lon":-122.43207694768259,"srid":4326}]},"endFeature":"signal","numLanes":2,"speedLimit":11.175999641418457}
 {"id":{"primary":170939,"secondary":0},"length":181.0661163330078,"startNodeId":{"primary":112513,"secondary":-1},"endNodeId":{"primary":112514,"secondary":-1},"geom":{"points":[{"lat":37.770776647388416,"lon":-122.41784890095028,"srid":4326},{"lat":37.77083564835014,"lon":-122.41787840143115,"srid":4326},{"lat":37.77089460925939,"lon":-122.41789805506755,"srid":4326},{"lat":37.77126455093812,"lon":-122.41801803559171,"srid":4326},{"lat":37.7717744891641,"lon":-122.41818801500037,"srid":4326},{"lat":37.77231436864929,"lon":-122.41835797706051,"srid":4326},{"lat":37.77235350744079,"lon":-122.41836773357855,"srid":4326}]},"endFeature":"nothing","numLanes":3,"speedLimit":11.175999641418457}
 {"id":{"primary":171797,"secondary":1},"length":146.4946746826172,"startNodeId":{"primary":171797,"secondary":1},"endNodeId":{"primary":171797,"secondary":2},"geom":{"points":[{"lat":37.79512778911096,"lon":-122.4283436173737,"srid":4326},{"lat":37.795127789101514,"lon":-122.42834361744747,"srid":4326},{"lat":37.79490778960546,"lon":-122.42998361749531,"srid":4326}]},"endFeature":"stop","numLanes":2,"speedLimit":11.175999641418457}
 {"id":{"primary":179992,"secondary":0},"length":105.68424987792969,"startNodeId":{"primary":116128,"secondary":-1},"endNodeId":{"primary":179992,"secondary":1},"geom":{"points":[{"lat":37.79765757122467,"lon":-122.40862250114678,"srid":4326},{"lat":37.79730757122467,"lon":-122.40856250114678,"srid":4326},{"lat":37.79671756906145,"lon":-122.40843251709266,"srid":4326}]},"endFeature":"signal","numLanes":2,"speedLimit":11.175999641418457}
 {"id":{"primary":168061,"secondary":0},"length":148.77975463867188,"startNodeId":{"primary":111257,"secondary":-1},"endNodeId":{"primary":107421,"secondary":-1},"geom":{"points":[{"lat":37.78174782124127,"lon":-122.42225335656971,"srid":4326},{"lat":37.781607821241266,"lon":-122.4234333565697,"srid":4326},{"lat":37.781547821442544,"lon":-122.42392335655714,"srid":4326}]},"endFeature":"signal","numLanes":3,"speedLimit":11.175999641418457}
 {"id":{"primary":313385,"secondary":0},"length":103.3366470336914,"startNodeId":{"primary":112474,"secondary":-1},"endNodeId":{"primary":102135,"secondary":-1},"geom":{"points":[{"lat":37.7893427646946,"lon":-122.41535756302162,"srid":4326},{"lat":37.7902627646946,"lon":-122.41553756302162,"srid":4326}]},"endFeature":"signal","numLanes":2,"speedLimit":11.175999641418457}
"""
  // scalastyle:on

  @Test def test1 = {
    val input = string.lines.toSeq.map(s => parse[GenericLinkRepresentation](s))
    val links = NetworkBuilder.build(input)
    assertEquals(7, links.size)
  }
}