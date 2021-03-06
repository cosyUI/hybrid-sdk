import WYAJSBridge from '../native.js';
import wya from '../web.js';
import { useDispatchEvent } from '../utils/utils';

describe('web.js', () => {
	window.WYAJSBridge = WYAJSBridge;
	let id = WYAJSBridge.count;

	test('验证api', () => {
		expect(typeof wya).toBe('object');
		expect(typeof wya.version).toBe('string');
		expect(typeof wya.invoke).toBe('function');
		expect(typeof wya.on).toBe('function');
		expect(typeof wya.once).toBe('function');
		expect(typeof wya.first).toBe('function');
		expect(typeof wya.last).toBe('function');
		expect(typeof wya.off).toBe('function');
		expect(typeof wya.onError).toBe('function');
		expect(typeof wya.onLog).toBe('function');
	});
	test('验证ready', () => {
		wya.ready(() => {
			expect(1).toBe(1);
		});

		// 模拟触发一次
		WYAJSBridge.emit('_ready_', {
			status: 1,
			data: {
				appVersion: '1.0.0' 
			}
		});

	});
	test('验证error', () => {
		wya.ready(() => {
			expect(1).toBe(1);
		});

		wya.onError((res) => {
			expect(res.msg).toBe('error');
		});

		wya.on('_error_', (res) => {
			expect(res.msg).toBe('error');
		});

		// Native 模拟触发
		WYAJSBridge.emit('_error_', {
			status: 0,
			msg: 'error',
			data: {}
		});

	});

	test('验证log', () => {
		wya.ready(() => {
			expect(1).toBe(1);
		});

		wya.onLog((res) => {
			expect(typeof res).toBe('string');
		});

		useDispatchEvent('成功');
	});
	test('验证event', () => {
		wya.on('batteryLow', (res) => {
			expect(res.level).toBe(20);
		});
		WYAJSBridge.emit(id, { 
			status: 1,
			msg: '成功打开',
			data: {
				
			}
		});
		id++;

		wya.once('batteryLow', (res) => {
			expect(res.tip).toBe('xxx');
		});

		// 再次订阅，之前没有取消，不会通知Native, id 不会增加
		expect(WYAJSBridge.count).toBe(id);

		// Native 模拟触发
		WYAJSBridge.emit('batteryLow', {
			status: 1,
			msg: 'success',
			data: {
				level: 20,
				tip: 'xxx'
			}
		});
		WYAJSBridge.off('batteryLow');
		WYAJSBridge.emit(id, { 
			status: 1,
			msg: '关闭成功',
			data: {
				
			}
		});
		id++; // 触发后增加''
		
		expect(WYAJSBridge.nativeEvents.includes('batteryLow')).toBe(false);
		expect(WYAJSBridge.count).toBe(id);
	});

	test('验证requireModule', () => {
		let navigator = wya.requireModule('navigator');
		navigator.push({ user: 'wya' })
			.then((res) => {
				expect(res.type).toBe("3g");
			})
			.catch((err) => {
				console.log(err);
			});
		expect(typeof WYAJSBridge.store[id]).toBe("object");
		expect(WYAJSBridge.store[id].scheme).toBe("navigator/push");

		let params = JSON.parse(WYAJSBridge.getParams(id));		
		expect(params.user).toBe("wya");

		WYAJSBridge.emit(id, { 
			status: 1,
			data: {
				type: '3g'
			}
		});

		expect(typeof WYAJSBridge.store[id]).toBe("undefined");

		id++; // 触发后增加

		expect(WYAJSBridge.count).toBe(id);
	});
});
