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

package com.jbrisbin.vpc.riak

/**
 *
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakObject<T> {

  String bucket
  String key
  String vclock
  Map userMeta = [:]
  List<Link> links = new ArrayList<Link>()
  Long lastModified
  String etag
  String contentType = "application/json"
  Boolean dirty = true
  Map data

  String getRelativeUrl() {
    StringBuilder buff = new StringBuilder(bucket)
    if (key) {
      buff << "/$key"
    }
    buff.toString()
  }

}
