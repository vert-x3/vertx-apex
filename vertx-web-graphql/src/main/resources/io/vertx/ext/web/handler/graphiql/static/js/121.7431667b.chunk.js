"use strict";(self.webpackChunkvertx_web_graphiql=self.webpackChunkvertx_web_graphiql||[]).push([[121],{2121:function(e,o,t){t.r(o);var n=t(1003),r=(t(7331),t(7313),t(1168),Object.defineProperty),i=function(e,o){return r(e,"name",{value:o,configurable:!0})};function u(e){return{options:e instanceof Function?{render:e}:!0===e?{}:e}}function a(e){var o=e.state.info.options;return(null===o||void 0===o?void 0:o.hoverTime)||500}function m(e,o){var t=e.state.info,r=o.target||o.srcElement;if(r instanceof HTMLElement&&"SPAN"===r.nodeName&&void 0===t.hoverTimeout){var u=r.getBoundingClientRect(),m=i((function(){clearTimeout(t.hoverTimeout),t.hoverTimeout=setTimeout(p,l)}),"onMouseMove"),s=i((function(){n.C.off(document,"mousemove",m),n.C.off(e.getWrapperElement(),"mouseout",s),clearTimeout(t.hoverTimeout),t.hoverTimeout=void 0}),"onMouseOut"),p=i((function(){n.C.off(document,"mousemove",m),n.C.off(e.getWrapperElement(),"mouseout",s),t.hoverTimeout=void 0,f(e,u)}),"onHover"),l=a(e);t.hoverTimeout=setTimeout(p,l),n.C.on(document,"mousemove",m),n.C.on(e.getWrapperElement(),"mouseout",s)}}function f(e,o){var t=e.coordsChar({left:(o.left+o.right)/2,top:(o.top+o.bottom)/2}),n=e.state.info.options,r=n.render||e.getHelper(t,"info");if(r){var i=e.getTokenAt(t,!0);if(i){var u=r(i,n,e,t);u&&s(e,o,u)}}}function s(e,o,t){var r=document.createElement("div");r.className="CodeMirror-info",r.appendChild(t),document.body.appendChild(r);var u=r.getBoundingClientRect(),a=window.getComputedStyle(r),m=u.right-u.left+parseFloat(a.marginLeft)+parseFloat(a.marginRight),f=u.bottom-u.top+parseFloat(a.marginTop)+parseFloat(a.marginBottom),s=o.bottom;f>window.innerHeight-o.bottom-15&&o.top>window.innerHeight-o.bottom&&(s=o.top-f),s<0&&(s=o.bottom);var p,l=Math.max(0,window.innerWidth-m-15);l>o.left&&(l=o.left),r.style.opacity="1",r.style.top=s+"px",r.style.left=l+"px";var v=i((function(){clearTimeout(p)}),"onMouseOverPopup"),c=i((function(){clearTimeout(p),p=setTimeout(d,200)}),"onMouseOut"),d=i((function(){n.C.off(r,"mouseover",v),n.C.off(r,"mouseout",c),n.C.off(e.getWrapperElement(),"mouseout",c),r.style.opacity?(r.style.opacity="0",setTimeout((function(){r.parentNode&&r.parentNode.removeChild(r)}),600)):r.parentNode&&r.parentNode.removeChild(r)}),"hidePopup");n.C.on(r,"mouseover",v),n.C.on(r,"mouseout",c),n.C.on(e.getWrapperElement(),"mouseout",c)}n.C.defineOption("info",!1,(function(e,o,t){if(t&&t!==n.C.Init){var r=e.state.info.onMouseOver;n.C.off(e.getWrapperElement(),"mouseover",r),clearTimeout(e.state.info.hoverTimeout),delete e.state.info}if(o){var i=e.state.info=u(o);i.onMouseOver=m.bind(null,e),n.C.on(e.getWrapperElement(),"mouseover",i.onMouseOver)}})),i(u,"createState"),i(a,"getHoverTime"),i(m,"onMouseOver"),i(f,"onMouseHover"),i(s,"showPopup")}}]);