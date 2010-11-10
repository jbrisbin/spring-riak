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
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.springframework.core.convert.converter.Converter

/**
 *
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class StringToLinkConverter implements Converter<String, Link> {

  Pattern regex = Pattern.compile("<(.+)/(.+)/(.+)>; riaktag=\"(.+)\"")

  Link convert(String source) {
    Matcher match = regex.matcher(source)
    if (match.matches()) {
      def prefix = match.group(1)
      def bucket = match.group(2)
      def key = match.group(3)
      def tag = match.group(4)
      new Link(prefix: prefix, bucket: bucket, key: key, tag: tag)
    } else {
      null
    }
  }

}
