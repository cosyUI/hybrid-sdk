import tpl from './modules/login';

export const loginConfig = [
	{ 
		path: '/login',
		name: 'login',
		component: () => import('./modules/login.vue') 
	}
];
