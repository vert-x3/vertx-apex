/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/** @module vertx-apex-addons-js/timeout */
var utils = require('vertx-js/util/utils');
var RoutingContext = require('vertx-apex-core-js/routing_context');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JTimeout = io.vertx.ext.apex.addons.Timeout;

/**

 @class
*/
var Timeout = function(j_val) {

  var j_timeout = j_val;
  var that = this;

  /**

   @public
   @param event {RoutingContext} 
   */
  this.handle = function(event) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'object' && __args[0]._jdel) {
      j_timeout.handle(event._jdel);
    } else utils.invalidArgs();
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_timeout;
};

/**

 @memberof module:vertx-apex-addons-js/timeout
 @param timeout {number} 
 @return {Timeout}
 */
Timeout.timeout = function() {
  var __args = arguments;
  if (__args.length === 0) {
    return new Timeout(JTimeout.timeout());
  }else if (__args.length === 1 && typeof __args[0] ==='number') {
    return new Timeout(JTimeout.timeout(__args[0]));
  } else utils.invalidArgs();
};

// We export the Constructor function
module.exports = Timeout;