"use strict";(self.webpackChunkvertx_web_graphiql=self.webpackChunkvertx_web_graphiql||[]).push([[238],{238:function(e,t,n){n.r(t);var a=n(1003),r=n(2369),u=(n(7331),n(7313),n(1168),Object.defineProperty),l=function(e,t){return u(e,"name",{value:t,configurable:!0})};function i(e,t){var n,a,r=e.levels;return((r&&0!==r.length?r[r.length-1]-((null===(n=this.electricInput)||void 0===n?void 0:n.test(t))?1:0):e.indentLevel)||0)*((null===(a=this.config)||void 0===a?void 0:a.indentUnit)||0)}a.C.defineMode("graphql-variables",(function(e){var t=(0,r.o)({eatWhitespace:function(e){return e.eatSpace()},lexRules:c,parseRules:s,editorConfig:{tabSize:e.tabSize}});return{config:e,startState:t.startState,token:t.token,indent:i,electricInput:/^\s*[}\]]/,fold:"brace",closeBrackets:{pairs:'[]{}""',explode:"[]{}"}}})),l(i,"indent");var c={Punctuation:/^\[|]|\{|\}|:|,/,Number:/^-?(?:0|(?:[1-9][0-9]*))(?:\.[0-9]*)?(?:[eE][+-]?[0-9]+)?/,String:/^"(?:[^"\\]|\\(?:"|\/|\\|b|f|n|r|t|u[0-9a-fA-F]{4}))*"?/,Keyword:/^true|false|null/},s={Document:[(0,r.p)("{"),(0,r.l)("Variable",(0,r.a)((0,r.p)(","))),(0,r.p)("}")],Variable:[o("variable"),(0,r.p)(":"),"Value"],Value:function(e){switch(e.kind){case"Number":return"NumberValue";case"String":return"StringValue";case"Punctuation":switch(e.value){case"[":return"ListValue";case"{":return"ObjectValue"}return null;case"Keyword":switch(e.value){case"true":case"false":return"BooleanValue";case"null":return"NullValue"}return null}},NumberValue:[(0,r.t)("Number","number")],StringValue:[(0,r.t)("String","string")],BooleanValue:[(0,r.t)("Keyword","builtin")],NullValue:[(0,r.t)("Keyword","keyword")],ListValue:[(0,r.p)("["),(0,r.l)("Value",(0,r.a)((0,r.p)(","))),(0,r.p)("]")],ObjectValue:[(0,r.p)("{"),(0,r.l)("ObjectField",(0,r.a)((0,r.p)(","))),(0,r.p)("}")],ObjectField:[o("attribute"),(0,r.p)(":"),"Value"]};function o(e){return{style:e,match:function(e){return"String"===e.kind},update:function(e,t){e.name=t.value.slice(1,-1)}}}l(o,"namedKey")}}]);