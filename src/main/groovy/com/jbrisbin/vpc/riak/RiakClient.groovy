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

import com.jbrisbin.vpc.riak.converter.RiakObjectHttpMessageConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.support.RestGatewaySupport

/**
 *
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakClient extends RestGatewaySupport {

  final Logger log = LoggerFactory.getLogger(this.class)
  String protocol = "http"
  String host = "localhost"
  int port = 8098
  String prefix = "/riak"
  RiakObjectHttpMessageConverter msgc

  RestTemplate getRestTemplate() {
    RestTemplate t = super.getRestTemplate();
    t.messageConverters.add(0, new RiakObjectHttpMessageConverter<RiakObject>())
    t
  }

  void setPrefix(String prefix) {
    if (prefix[0] != '/') {
      this.prefix = "/$prefix"
    } else {
      this.prefix = prefix
    }
    if (this.prefix[-1] == '/') {
      this.prefix = this.prefix[0..-2]
    }
  }

  String getUrl() {
    def url = "$protocol://$host:$port$prefix/{bucket}/{key}"
    if (log.debugEnabled) {
      log.debug("Using URL: $url")
    }
    url
  }

  String getUrl(Map params) {
    StringBuilder buff = new StringBuilder(getUrl())
    def query = params.collect { k, v ->
      [URLEncoder.encode(k, "UTF-8"), URLEncoder.encode(v?.toString(), "UTF-8")].join("=")
    }.join("&")
    if (query) {
      buff << "?" << query
    }
    return buff.toString()
  }

  String getRelativeUrl(bucket, key) {
    def url = "$prefix/$bucket/$key"
    if (log.debugEnabled) {
      log.debug("Using relative URL: $url")
    }
    url
  }

  def List<?> getKeys(bucket) {
    def o = getRestTemplate().getForObject(getUrl([keys: true]), Map, bucket, "")
    if (log.debugEnabled) {
      log.debug("getKeys: $o")
    }
    o.keys
  }

  def <T> T retrieve(key, Class<T> targetType) {
    return retrieve(targetType.name, key, targetType)
  }

  def <T> T retrieve(bucket, key, Class<T> targetType) {
    def o
    try {
      o = getRestTemplate().getForObject(url, targetType, bucket, key)
      if (targetType == RiakObject) {
        o.bucket = bucket
        o.key = key
      }
    } catch (HttpClientErrorException e) {
    }
    if (log.debugEnabled) {
      log.debug("Retrieve: $o")
    }
    o
  }

  def <T> T retrieve(bucket, key, Class<T> targetType, ResponseEntityCallback<ResponseEntity<T>> callback) {
    def entity = getRestTemplate().getForEntity(url, targetType, bucket, key)
    callback?.doWithEntity(entity)
  }

  List<?> retrieveAll(Class<?> targetType) {
    return retrieveAll(targetType.name)
  }

  List<?> retrieveAll(bucket, Class<?> targetType) {
    getKeys(bucket).collect {
      retrieve(bucket, it, targetType)
    }
  }

  List retrieveAll(String bucket) {
    retrieveAll(bucket, Map)
  }

  Map retrieveHeaders(bucket, key) {
    def entity = getRestTemplate().getForEntity(url, Map, bucket, key)
      entity?.headers ?: [:]
  }

  List<Link> retrieveLinks(bucket, key) {
    def entity = getRestTemplate().getForEntity(url, Map, bucket, key)
    entity.headers.get("Link").collect {
      Link.linkFromHeader(this, it)
    }
  }

  def store(obj, bucket, key) {
    HttpHeaders headers = new HttpHeaders(contentType: MediaType.APPLICATION_JSON)
    HttpEntity entity = new HttpEntity(obj, headers)
    getRestTemplate().put(url, entity, bucket, key)
    if (log.debugEnabled) {
      log.debug("Store: $obj")
    }
    obj
  }

  void link(RiakObject linkSrc, RiakObject linkTgt, String name) {
    def link = getRelativeUrl(linkTgt.bucket, linkTgt.key)
    HttpHeaders headers = new HttpHeaders()
    boolean alreadyLinked = false
    linkSrc.links.each {
      headers.add("Link", it)
      alreadyLinked = (alreadyLinked || it.contains(link))
    }
    if (!alreadyLinked) {
      if (log.debugEnabled) {
        log.debug "link: ${link}"
      }
      headers.add("Link", "<$link>; riaktag=\"$name\"")
    }

    HttpEntity entity = new HttpEntity(linkSrc.data, headers)
    getRestTemplate().put(url, entity, linkSrc.bucket, linkSrc.key)
  }

  void delete(bucket, key) {
    getRestTemplate().delete(url, bucket, key)
  }

}
