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

package com.jbrisbin.vpc.riak.converter

import com.jbrisbin.vpc.riak.Link
import com.jbrisbin.vpc.riak.RiakObject
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter

/**
 *
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakObjectHttpMessageConverter extends MappingJacksonHttpMessageConverter {

  Pattern regex = Pattern.compile("<(.+)/(.+)/(.+)>; riaktag=\"(.+)\"")

  def boolean canRead(Class<?> clazz, MediaType mediaType) {
    return (clazz == RiakObject) && super.canRead(mediaType)
  }

  protected boolean supports(Class<?> clazz) {
    return clazz == RiakObject
  }

  protected RiakObject readInternal(Class<? extends RiakObject> clazz, HttpInputMessage inputMessage) {
    RiakObject obj = new RiakObject()
    // Set various properties
    obj.contentType = inputMessage.headers.contentType
    obj.vclock = inputMessage.headers["X-Riak-Vclock"]
    obj.lastModified = inputMessage.headers.lastModified
    obj.etag = inputMessage.headers.ETag

    // Do links
    inputMessage.headers["Link"][0].split(",").each { link ->
      Matcher match = regex.matcher(link.trim())
      if (match.matches()) {
        def prefix = match.group(1)
        def bucket = match.group(2)
        def key = match.group(3)
        def tag = match.group(4)
        obj.links << new Link(prefix: prefix, bucket: bucket, key: key, tag: tag)
      }
    }

    // Parse body
    obj.data = super.readInternal(Map, inputMessage)

    obj
  }

  protected void writeInternal(RiakObject t, HttpOutputMessage outputMessage) {
    outputMessage.headers.contentType = t.contentType
    outputMessage.headers.ifModifiedSince = t.lastModified
    outputMessage.headers.ifNoneMatch = t.etag
    outputMessage.headers["Link"] = t.links.collect { it.toString() }
    super.writeInternal(t.data, outputMessage)
  }

}
