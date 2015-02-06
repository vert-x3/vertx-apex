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

/** @module vertx-apex-core-js/route */
var utils = require('vertx-js/util/utils');
var RoutingContext = require('vertx-apex-core-js/routing_context');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JRoute = io.vertx.ext.apex.core.Route;

/**

 @class
*/
var Route = function(j_val) {

  var j_route = j_val;
  var that = this;

  /**

   @public
   @param method {Object} 
   @return {Route}
   */
  this.method = function(method) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'string') {
      return new Route(j_route.method(io.vertx.core.http.HttpMethod.valueOf(__args[0])));
    } else utils.invalidArgs();
  };

  /**

   @public
   @param path {string} 
   @return {Route}
   */
  this.path = function(path) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'string') {
      return new Route(j_route.path(path));
    } else utils.invalidArgs();
  };

  /**

   @public
   @param path {string} 
   @return {Route}
   */
  this.pathRegex = function(path) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'string') {
      return new Route(j_route.pathRegex(path));
    } else utils.invalidArgs();
  };

  /**

   @public
   @param contentType {string} 
   @return {Route}
   */
  this.produces = function(contentType) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'string') {
      return new Route(j_route.produces(contentType));
    } else utils.invalidArgs();
  };

  /**

   @public
   @param contentType {string} 
   @return {Route}
   */
  this.consumes = function(contentType) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'string') {
      return new Route(j_route.consumes(contentType));
    } else utils.invalidArgs();
  };

  /**

   @public
   @param order {number} 
   @return {Route}
   */
  this.order = function(order) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] ==='number') {
      return new Route(j_route.order(order));
    } else utils.invalidArgs();
  };

  /**

   @public
   @param last {boolean} 
   @return {Route}
   */
  this.last = function(last) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] ==='boolean') {
      return new Route(j_route.last(last));
    } else utils.invalidArgs();
  };

  /**

   @public
   @param requestHandler {function} 
   @return {Route}
   */
  this.handler = function(requestHandler) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'function') {
      return new Route(j_route.handler(function(jVal) {
      requestHandler(new RoutingContext(jVal));
    }));
    } else utils.invalidArgs();
  };

  /**

   @public
   @param failureHandler {function} 
   @return {Route}
   */
  this.failureHandler = function(failureHandler) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'function') {
      return new Route(j_route.failureHandler(function(jVal) {
      failureHandler(new RoutingContext(jVal));
    }));
    } else utils.invalidArgs();
  };

  /**

   @public

   @return {Route}
   */
  this.remove = function() {
    var __args = arguments;
    if (__args.length === 0) {
      return new Route(j_route.remove());
    } else utils.invalidArgs();
  };

  /**

   @public

   @return {Route}
   */
  this.disable = function() {
    var __args = arguments;
    if (__args.length === 0) {
      return new Route(j_route.disable());
    } else utils.invalidArgs();
  };

  /**

   @public

   @return {Route}
   */
  this.enable = function() {
    var __args = arguments;
    if (__args.length === 0) {
      return new Route(j_route.enable());
    } else utils.invalidArgs();
  };

  /**

   @public

   @return {string}
   */
  this.getPath = function() {
    var __args = arguments;
    if (__args.length === 0) {
      return j_route.getPath();
    } else utils.invalidArgs();
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_route;
};

// We export the Constructor function
module.exports = Route;