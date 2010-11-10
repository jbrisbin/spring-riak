/*
 * Copyright (c) 2010 by J. Brisbin <jon@jbrisbin.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jbrisbin.vpc.test.riak

import com.jbrisbin.vpc.riak.ResponseEntityCallback
import com.jbrisbin.vpc.riak.RiakClient
import com.jbrisbin.vpc.riak.RiakObject
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

/**
 *
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakClientSpec extends Specification {

  RiakClient riak

  def setup() {
    riak = new RiakClient(restTemplate: new RestTemplate(), prefix: '/riak')
  }

  void "Test storing object"() {
    given:
      def t = [test: "value"]
      def t2 = [test: "value2"]

    when:
      def o = riak.store(t, "test", "test")
      riak.store(t2, "test", "test2")

    then:
      o.test == "value"
  }

  void "Test fetching test object"() {
    given:
      def t

    when:
      t = riak.retrieve("test", "test", RiakObject)

    then:
      t.bucket == "test"
  }

  void "Test callback"() {
    given:
      def t
      def callback = [doWithEntity: { ent ->
        println "headers: ${ent.headers.entrySet()}"
        [test: "from closure"]
      }] as ResponseEntityCallback<Map>

    when:
      t = riak.retrieve("test", "test", Map, callback)

    then:
      t.test == "from closure"
  }

  void "Test fetching list of test objects"() {
    given:
      def t

    when:
      t = riak.retrieveAll("test")

    then:
      2 == t.size()
  }

  void "Test linking"() {
    given:
      def t
      def r1 = riak.retrieve("test", "test", RiakObject)
      def r2 = riak.retrieve("test", "test2", RiakObject)

    when:
      riak.link(r1, r2, "test")
      def r3 = riak.retrieve("test", "test", RiakObject)

    then:
      1 == r3.links.size()
  }

  void "Test deleting object"() {
    when:
      riak.delete("test", "test")
      riak.delete("test", "test2")
      def e = riak.retrieve("test", "test", Map)

    then:
      null == e
  }

}
