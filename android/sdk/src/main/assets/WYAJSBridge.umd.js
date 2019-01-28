! function(t, e) {
	"object" == typeof exports && "undefined" != typeof module ? module.exports = e() : "function" == typeof define && define.amd ? define(e) : t.WYAJSBridge = e()
}(this, function() {
	"use strict";
	var a = function(t, e) {
			r("_error_", t), e && e({
				status: 0,
				msg: t
			})
		},
		r = function(t, e) {
			var n = void 0;
			"function" == typeof document.CustomEvent ? n = new document.CustomEvent(t, {
				bubbles: !0,
				cancelable: !0
			}) : "function" == typeof document.createEvent && (n = document.createEvent("Event")).initEvent(t, !0, !0), e && n && (n.data = e), n ? window.dispatchEvent(n) : console.error("Bridge Error: dispatchEvent")
		},
		u = "function" == typeof Symbol && "symbol" == typeof Symbol.iterator ? function(t) {
			return typeof t
		} : function(t) {
			return t && "function" == typeof Symbol && t.constructor === Symbol && t !== Symbol.prototype ? "symbol" : typeof t
		},
		i = function(t, e) {
			if (!(t instanceof e)) throw new TypeError("Cannot call a class as a function")
		},
		e = function() {
			function i(t, e) {
				for (var n = 0; n < e.length; n++) {
					var i = e[n];
					i.enumerable = i.enumerable || !1, i.configurable = !0, "value" in i && (i.writable = !0), Object.defineProperty(t, i.key, i)
				}
			}
			return function(t, e, n) {
				return e && i(t.prototype, e), n && i(t, n), t
			}
		}(),
		o = Object.assign || function(t) {
			for (var e = 1; e < arguments.length; e++) {
				var n = arguments[e];
				for (var i in n) Object.prototype.hasOwnProperty.call(n, i) && (t[i] = n[i])
			}
			return t
		},
		n = function() {
			function n() {
				var t = 0 < arguments.length && void 0 !== arguments[0] ? arguments[0] : {};
				if (i(this, n), "object" !== (void 0 === t ? "undefined" : u(t)) || t instanceof n || t.__events__ || t.__listeners__ || t.on || t.off || t.emit) throw new TypeError("不符合观察条件，请删除以下对象.\n\t\t\t\t\n__events__\n__listeners__\non\noff\nemit\n\t\t\t");
				for (var e in n.prototype.__events__ = {}, n.prototype.__listeners__ = [], t) this[e] = t[e]
			}
			return e(n, [{
				key: "on",
				value: function(t, e) {
					if ("object" === (void 0 === t ? "undefined" : u(t)))
						for (key in t) t.hasOwnProperty(key) && "function" == typeof t[key] && this.on(key, t[key]);
					else "string" == typeof t && "function" == typeof e ? (this.__events__[t] || (this.__events__[t] = []), this.__events__[t].push(e)) : "function" == typeof t && this.__listeners__.push(t);
					return this
				}
			}, {
				key: "off",
				value: function(t) {
					return "string" == typeof t ? delete this.__events__[t] : void 0 === (void 0 === t ? "undefined" : u(t)) && (this.__listeners__ = []), this
				}
			}, {
				key: "once",
				value: function(e, n) {
					var i = this;
					if ("string" == typeof e && (!this.__events__[e] || 0 === this.__events__[e].length)) {
						var o = !1;
						this.on(e, function(t) {
							i.off(e), o || (o = !0, n.call(i, t))
						})
					}
					return this
				}
			}, {
				key: "emit",
				value: function(t) {
					var e = 1 < arguments.length && void 0 !== arguments[1] ? arguments[1] : {};
					if (e instanceof Array || "object" !== (void 0 === e ? "undefined" : u(e)) || e.event) return a(t + "事件：回调参数必须是对象, 且不能让带event关键字"), this;
					if (!this.__events__[t] || 0 === this.__events__[t].length) return a(t + "事件：没有任何回调"), this;
					if ("string" == typeof t && this.__events__.hasOwnProperty(t) && this.__events__[t] instanceof Array)
						for (var n = 0; this.__events__[t] && n < this.__events__[t].length && !1 !== this.__events__[t][n].call(this, e); n++);
					for (var i = 0; i < this.__listeners__.length && !1 !== this.__listeners__[i].call(this, o({}, e, {
							event: t
						})); i++);
					return this
				}
			}]), n
		}();
	return new(function() {
		function t() {
			i(this, t), this.version = "1.0.0", this.store = {}, this.count = 0, this.source = new n, this.initCount = 0, this.waitEmit = []
		}
		return e(t, [{
			key: "_send",
			value: function(e, n) {
				(this.store[e] || {}).eventName;
				setTimeout(function() {
					var t = document.createElement("iframe");
					t.src = "command://" + n + "?id=" + e, t.style.display = "none", document.body.appendChild(t), setTimeout(function() {
						t.parentNode.removeChild(t)
					}, 300)
				}, 0)
			}
		}, {
			key: "getParam",
			value: function(t) {
				return (this.store[t] || {}).params
			}
		}, {
			key: "invoke",
			value: function(n, r) {
				var s = this;
				return new Promise(function(i, o) {
					if (n) {
						r = r ? decodeURIComponent(JSON.stringify(r)) : "";
						var t = s.count++;
						s.store[t] = {
							params: r,
							eventName: n + "__" + t
						};
						var e = s.store[t].eventName;
						s.source.once(e, function(t) {
							var e = t.data,
								n = t.id;
							1 == t.status ? i(e) : o(e), delete s.store[n]
						}), 0 === s.initCount ? s.waitEmit.push(s._send.bind(s, t, n)) : s._send(t, n)
					} else a("scheme 方法名必传", o)
				})
			}
		}, {
			key: "addEventListener",
			value: function(t, e) {
				for (var n = arguments.length, i = Array(2 < n ? n - 2 : 0), o = 2; o < n; o++) i[o - 2] = arguments[o];
				var r = ("object" === u(i[0]) ? i[0].success : i[0]) || function() {},
					s = ("object" === u(i[1]) ? i[0].fail : i[1]) || function() {};
				this.source[t](e, function(t) {
					var e = t.data;
					t.id;
					1 == t.status ? r(e) : s(e)
				})
			}
		}, {
			key: "on",
			value: function(t) {
				for (var e = arguments.length, n = Array(1 < e ? e - 1 : 0), i = 1; i < e; i++) n[i - 1] = arguments[i];
				this.addEventListener.apply(this, ["on", t].concat(n))
			}
		}, {
			key: "once",
			value: function(t) {
				for (var e = arguments.length, n = Array(1 < e ? e - 1 : 0), i = 1; i < e; i++) n[i - 1] = arguments[i];
				this.addEventListener.apply(this, ["once", t].concat(n))
			}
		}, {
			key: "last",
			value: function(t) {
				this.off(t);
				for (var e = arguments.length, n = Array(1 < e ? e - 1 : 0), i = 1; i < e; i++) n[i - 1] = arguments[i];
				this.on.apply(this, [t].concat(n))
			}
		}, {
			key: "first",
			value: function(t) {
				if (this.source.__events__[t] || 0 === this.source.__events__[t].length) {
					for (var e = arguments.length, n = Array(1 < e ? e - 1 : 0), i = 1; i < e; i++) n[i - 1] = arguments[i];
					this.on.apply(this, [t].concat(n))
				} else {
					a(t + "只执行触发一次绑定")
				}
			}
		}, {
			key: "emit",
			value: function(t, e) {
				console.log(e);
				var n = (this.store[t] || {}).eventName,
					i = void 0 === n ? t : n;
				if ("_error_" !== i && "string" == typeof e) try {
					void 0 === (e = JSON.parse(e) || {}).status && a("Native端返回格式错误: " + e)
				} catch (t) {
					a("Native端返回格式错误: " + e)
				}
				switch (i) {
					case "_ready_":
						if (this.initCount++, 1 === this.initCount) r(i, e), this.waitEmit.forEach(function(t, e) {
							return t()
						}), this.waitEmit = [];
						else {
							var o = "_ready_ 事件, 只允许初始化一次";
							r(i, {
								status: 0,
								msg: o
							}), a(o)
						}
						break;
					case "_error_":
						a(e);
					default:
						this.source.emit(i, e)
				}
			}
		}, {
			key: "off",
			value: function(t) {
				this.source.off(t)
			}
		}]), t
	}())
});
