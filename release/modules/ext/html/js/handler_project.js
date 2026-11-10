/**
 * КЛАСС ПРЕДСТАВЛЕНИЯ (View)
 * Чистая структура DOM и управление движком jsMind. Никакого CSS в коде.
 */

class ProjectMapManager {
	/**
	 * @param {jQuery} $parentContainer Родительский контейнер (обычно body)
	 */
	constructor($parentContainer) {
		window.App.ProjectMapManager = this;
		this.$parent = $parentContainer;
		this.jm = null;
		this.containerId = $.getGUID();
		this.isDirty = false;
		this.lastSelectedNode = null;
		this.currentInspectorView = null;

		this._buildLayout();
		this._initEngine();
		this.updateStatesUI();
	}
		_buildLayout() {
		const $layout = $('<div>', {'class': 'mindmanager-layout'});
		const $panel = this.$toolbar = $('<div>', {'class': 'mindmanager-panel', 'id': 'bottom-panel'});
		const $toolbar = $('<div>', {'class': 'mm-toolbar'}).appendTo($panel);
		const $canvas = $('<div>', {'id': this.containerId, 'class': 'mm-canvas-container'});

		this.$btnSave = $toolbar.mapAddToolButton('save_map', () => this.exportAction());
		$('<div>', {'class': 'mm-toolbar-spacer'}).appendTo($toolbar);
		this.$btnRemove = $toolbar.mapAddToolButton('remove_node');
		this.$btnRemove.AddConfirm('remove_node_confirm', {
				position: [[{
					my: 'bottom',
					at: 'top',
					of: $panel
				}, {
					my: 'left',
					at: 'left',
					of: this.$btnRemove
				}],{
					collision: 'flipfit', // 'none', 'fit', 'flip', 'flipfit'
					within: window
				}]
			}, () => this.removeNodeAction());
		this.$btnAdd = $toolbar.mapAddToolButton('add_node', () => this.addNodeAction());

		$layout.append($canvas).append($panel);	
		this.$inspectorPanel = $('<div>', {'class': 'mm-node-inspector'}).appendTo($panel);

		this.$parent.empty().append($layout);
	}

	_initEngine() {
		const that = this;
		const options = {
			container: this.containerId,
			theme: 'netbeans',
			editable: true,
			onRenderNode: function(node, nodeElement) {
				return that.decorateNodeElement.apply(that, arguments);
			}
		};
		this.jm = new jsMind(options);


		// Перехватываем перемещение узлов для отслеживания изменений
		const originalMoveNode = this.jm.move_node;
		this.jm.move_node = function(...args) {
			const result = originalMoveNode.apply(this, args);
			that.isDirty = true;
			that.updateStatesUI();
			return result;
		};
		// Слушаем события выделения узла
		this.jm.add_event_listener((type, data) => {
			if (type === jsMind.event_type.select) {
				this.lastSelectedNode = this.jm.get_selected_node();
        this.updateStatesUI();
	    }
			if (type === jsMind.event_type.show && (data.evt === 'expand_node' || data.evt === 'collapse_node')) {
				this.isDirty = true;
				if (data.evt === 'expand_node') {
					const node = this.jm.get_node(data.node);
					if (node) {
						const container = this.jm.view.e_panel;
						const currentScrollLeft = container ? container.scrollLeft : 0;
						const currentScrollTop = container ? container.scrollTop : 0;
						this.jm.view.reset_node_custom_style(node);				
						setTimeout(() => {
							this.jm.layout.cache_valid = false;
							this.jm.view.init_nodes_size();
							this.jm.layout.part_layout(node);
							this.jm.view.show(true);
							if (container) {
								container.scrollLeft = currentScrollLeft;
								container.scrollTop = currentScrollTop;
							}
						}, 30);
					}
				}
				this.updateStatesUI();
			}
			if (type === jsMind.event_type.edit && data.node) {
				var node = this.jm.get_node(data.node);
				const $nodeElement = $(`jmnode[nodeid="${data.node}"]`);
				
				if ($nodeElement.exists()) {
					this.decorateNodeElement(node, $nodeElement[0]);
					if (node._data && node._data.view) {
						node._data.view.width = null;
						node._data.view.height = null;
					}
					setTimeout(() => {
						that.jm.layout.cache_valid = false;
						that.jm.view.init_nodes_size();
						that.jm.resize();
					}, 50);
				}
			}
			this.updateStatesUI();
		});
		this.$parent.on('node:changed', (e, node) => {
			this.isDirty = true;
			this.updateStatesUI();
		});
	}

	updateStatesUI() {
		const selected = this.jm ? this.jm.get_selected_node() : null;

		if (this.$btnAdd) {
			this.$btnAdd.prop('disabled', !selected);
		}
		if (this.$btnRemove) {
			this.$btnRemove.prop('disabled', !selected || selected.isroot);
		}
		if (this.$btnSave) {
			this.$btnSave.prop('disabled', !this.isDirty);
		}
		// 2. Рулим состоянием инспектора
		if (selected) {
			// Если узел выделен — определяем класс, создаем экземпляр и рендерим
			const type = selected.data?.data?.type || 'note';
			const ViewClass = window.App.NodeViewFactory.getClass(type);

			this.currentInspectorView = new ViewClass(selected, this.jm);
			this.$inspectorPanel.show();
			this.currentInspectorView.showInspector(this.$inspectorPanel);
		} else {
			// Выделения нет — очищаем и скрываем панель
			if (this.$inspectorPanel) {
				this.$inspectorPanel.hide().empty();
			}
			this.currentInspectorView = null;
		}
	}

	/**
	 * Визуализация flat-массива
	 */
		render(nodeArray) {
		if (!this.jm) return;
		
		this.isDirty = false;

		const mind = {
			meta: {name: 'CrazyGerman', author: 'Alexey', version: '1.0'},
			format: 'node_array',
			data: nodeArray
		};

		this.jm.show(mind);

		const that = this;
		setTimeout(() => {
			that.jm.layout.cache_valid = false;
			that.jm.view.init_nodes_size();
			that.jm.resize();
			that.updateStatesUI();
		}, 100);

		this.initCanvasPan();
		this.updateStatesUI();
	}

	getRawData() {
		if (!this.jm) return [];
		return this.jm.data.get_data('node_array').data;
	}

	addNodeAction() {
		const selected = this.jm.get_selected_node();
		if (!selected) {
			return;
		}

		const topic = MapUiConfig.getText('default_node_text');
		if (!topic) return;

		const newNodeId = $.getGUID();
		const node = this.jm.add_node(selected, newNodeId, topic);
		if (node) {
			this.isDirty = true;
			this.jm.select_node(newNodeId);
			this.scrollNodeToView(node);
			this.updateStatesUI();
		}
	}
	
	decorateNodeElement(node, nodeElement) {
		const type = node.data?.data?.type;
		window.App.NodeViewFactory.getClass(type).decorate(node, nodeElement);
	}
		
	scrollNodeToView(node) {
		if (!node || !node._data || !node._data.view || !node._data.view.element) return;
		const el = node._data.view.element;
		const container = this.jm.view.e_panel;
		if (!container) return;

		const nodeLeft = el.offsetLeft;
		const nodeTop = el.offsetTop;
		const nodeWidth = el.clientWidth;
		const nodeHeight = el.clientHeight;

		const containerWidth = container.clientWidth;
		const containerHeight = container.clientHeight;

		const targetLeft = nodeLeft - (containerWidth - nodeWidth) / 2;
		const targetTop = nodeTop - (containerHeight - nodeHeight) / 2;

		if (typeof container.scrollTo === 'function') {
			container.scrollTo({
				left: targetLeft,
				top: targetTop,
				behavior: 'smooth'
			});
		} else {
			container.scrollLeft = targetLeft;
			container.scrollTop = targetTop;
		}
	}
  
	removeNodeAction() {
		const selected = this.jm.get_selected_node();
		if (!selected || selected.isroot) {
			return;
		}
		this.jm.remove_node(selected);
		this.isDirty = true;
		this.updateStatesUI();
	}

	exportAction() {
		if (!this.isDirty) return;
		window.Handler.saveMapData({'command-set':'map-tree', nodes: this.getRawData()}, function(result){
			if (typeof(result) == 'object' && result.success == true) {
				this.isDirty = false;
				this.updateStatesUI();
			}
		}.bind(this));
	}
	
	initCanvasPan() {
		const container = document.querySelector('.jsmind-inner');
		if (!container) return;

		let isDown = false;
		let wasDragging = false;
		let startX, startY, scrollLeft, scrollTop;

		container.addEventListener('mousedown', (e) => {
			if (e.target.closest('jmnode') || e.target.closest('jmexpander')) return;
			isDown = true;
			wasDragging = false;
			container.style.cursor = 'grabbing';

			startX = e.pageX - container.offsetLeft;
			startY = e.pageY - container.offsetTop;
			scrollLeft = container.scrollLeft;
			scrollTop = container.scrollTop;
		});

		container.addEventListener('mouseleave', () => {
			isDown = false;
			container.style.cursor = 'default';
		});

		container.addEventListener('mouseup', () => {
			isDown = false;
			container.style.cursor = 'default';
			
			// Если мы просто тащили карту и отпустили мышь — возвращаем выделение узла взад
			if (wasDragging && this.lastSelectedNode) {
				this.jm.select_node(this.lastSelectedNode);
				this.updateStatesUI();
			}
		});

		container.addEventListener('mousemove', (e) => {
			if (!isDown) return;
			e.preventDefault();

			const x = e.pageX - container.offsetLeft;
			const y = e.pageY - container.offsetTop;

			const walkX = (x - startX) * 1.5;
			const walkY = (y - startY) * 1.5;

			// Если сдвинулись — значит, идет перетаскивание полотна
			if (Math.abs(walkX) > 3 || Math.abs(walkY) > 3) {
				wasDragging = true;
			}

			container.scrollLeft = scrollLeft - walkX;
			container.scrollTop = scrollTop - walkY;
		});

		// Наш контролируемый клик для деселекта
		$(`#${this.containerId}`).on('click', (e) => {
			if (wasDragging || $(e.target).closest('jmnode').length) {
				wasDragging = false;
				return;
			}
			this.lastSelectedNode = this.jm.get_selected_node();
			// Если это был чистый клик по пустому месту — сбрасываем выделение
			// (Оно к этому моменту уже сброшено движком, просто обновляем интерфейс)
			this.updateStatesUI();
		});
	}
		
}

// ============================================================================

/**
 * КЛАСС КОНТРОЛЛЕРА (Бизнес-логика и связь с бэком)
 */
function _MainHandler() {
	this.mapManager = new ProjectMapManager($('body'));
	this.getMapData({'command-get': 'map-tree'}, function (nodeArray) {
		this.mapManager.render(nodeArray);
	});
}

_MainHandler.prototype.getMapData = function (form, callback) {
	let nodeArray = [];
	try {
		let jsonFormString = JSON.stringify(form);
		let jsonDataString = window.Backend.getMapData(jsonFormString);
		nodeArray = JSON.parse(jsonDataString);
	} catch (e) {
		console.error(e);
		this.writeToConsole('getMapData. Ошибка парсинга: ' + e.message);
	}
	if (typeof (callback) == 'function') {
		callback.call(this, nodeArray);
	}
};

_MainHandler.prototype.saveMapData = function (form, callback) {
	let result = false;
	try {
		let jsonFormString = JSON.stringify(form);
		let jsonDataString = window.Backend.saveMapData(jsonFormString);
		result = JSON.parse(jsonDataString);
	} catch (e) {
		console.error(e);
		this.writeToConsole('saveMapData. Ошибка парсинга: ' + e.message);
	}
	if (typeof (callback) == 'function') {
		callback.call(this, result);
	}
};

_MainHandler.prototype.selectFile = function (type, callback) {
	const fnName = type == 'file' ? 'selectFile' : 'selectFolder';
	let result = false;
	try {
		result = window.Backend[fnName]();
	} catch (e) {
		console.error(e);
		this.writeToConsole('selectFile. Ошибка: ' + e.message);
	}
	if (typeof(result) == 'string' && typeof (callback) == 'function') {
		callback.call(this, result.trim());
	}
};

_MainHandler.prototype.writeToConsole = function () {
	if (window.Backend && typeof window.Backend.writeToConsole === 'function') {
		window.Backend.writeToConsole.apply(window.Backend, arguments);
	} else {
		console.info.apply(console, arguments);
	}
};

// Входной лисенер
$(document).on('startWork', function () {
	(function(window, callback) {
    'use strict';
		var environment = {};
		try {
			environment = JSON.parse(window.Backend.getEnvironment());
		} catch (err) {
			console.error(err);
		}
		if (typeof(callback) == 'function') {
			callback.call(this, window, environment);
		}
	})(window, function(window, environmentData){
    'use strict';
		//console.info('environmentData', environmentData);
		window.crazy74.lang.init(environmentData.lang_code, environmentData.lang_data);
		$('body').removeClass('loading');
		window.Handler = new _MainHandler();
		//window.Handler.writeToConsole('Frontend готов');
	});
});
