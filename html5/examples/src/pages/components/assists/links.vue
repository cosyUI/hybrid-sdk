<template>
	<div>
		<cell 
			v-for="(item, key) in routes" 
			:key="key" 
			v-bind="item"
		/>
	</div>
</template>

<script>
import Cell from '@common/cell/cell';
import * as rootConfig from './detail/modules/root';

export default {
	name: 'assists',
	components: {
		Cell
	},
	data() {
		return {
			routes: rootConfig
		};
	},
	created() {
		// console.log('?');
	},
	methods: {
	},
};
</script>

<style lang="scss" scoped>
</style>
