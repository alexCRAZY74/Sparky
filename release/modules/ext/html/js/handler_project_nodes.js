window.App = window.App || {};

class BaseNodeView {
	constructor(node, jm, existingBuffer = null) {
		this.node = node;
		this.jm = jm;
		this.isDetailed = false;
		this.buffer = existingBuffer || {
			topic: node.topic,
			type: node.data?.data?.type || 'default',
			meta: $.extend(true, {}, node.data?.data || {})
		};
		this.buffer.meta.type = this.buffer.type;
		this.extendModeAble = false;
	}

	static decorate(node, nodeElement) {
		console.clear();
		const meta = node.data.data;
		console.info(meta);
		const originalText = node.topic;
		nodeElement.innerHTML = `<div class="mm-node-title">${originalText}</div>`;
		const $node = $(nodeElement);

		$node.removeClass((index, className) => {
			return (className.match(/(^|\s)type-\S+/g) || []).join(' ');
		});

		//if (!meta || !meta.type || meta.type === 'default') return;
		if (typeof(meta) !== 'object') return;
		if (!meta || !meta.type) {
			meta.type = 'default';
		}
		
		$node.addClass(`type-${meta.type}`);
		if (typeof(meta.date) == 'string') {
			$('<div>', {
				'class': 'mm-node-dates',
			}).SmartTime(meta.date, {withTime: false}).appendTo($node);
		}
		if (typeof(meta.datetime) == 'string') {
			$('<div>', {
				'class': 'mm-node-dates',
			}).SmartTime(meta.datetime, {withTime: true}).appendTo($node);
		}
		if (meta && typeof(meta.dates) === 'object' && (meta.dates.start || meta.dates.end)) {
			const $datesContainer = $('<div>', {
				'class': 'mm-node-dates'
			}).appendTo($node);

			if (meta.dates.start) {
				$('<span>', { 'class': 'mm-date-start' })
					.SmartTime(meta.dates.start, { withTime: false })
					.appendTo($datesContainer);
			}

			if (meta.dates.start && meta.dates.end) {
				$datesContainer.append(' — ');
			}

			if (meta.dates.end) {
				$('<span>', { 'class': 'mm-date-end' })
					.SmartTime(meta.dates.end, { withTime: false })
					.appendTo($datesContainer);
			}
		}
		
		(this.makeBadges = () => {
			if (Array.isArray(meta.kanbans) && meta.kanbans.length > 0) {
				const rootNode = window.App.ProjectMapManager?.jm?.get_root();
				const settings = rootNode?.data?.data?.kanban_settings;

				if (settings && Array.isArray(settings.lists)) {
					const $badgeContainer = $('<div class="mm-node-badges"></div>');

					settings.lists.forEach(currentList => {
						const kb = meta.kanbans.find(k => k.code === currentList.code);
						if (!kb) return;
						const option = currentList.options.find(o => o.value === kb.value);
						if (!option) return;

						const iconHtml = option.icon ? `<span class="icon ${option.icon}" title="${currentList.title}: ${option.text}"></span> ` : '';

						let kanban_sign = $('<span>', {
							'class': `mm-badge badge-${kb.code} badge-value-${kb.value}`,
							'html': `${iconHtml}<span class="label">${option.text}</span>`
						}).appendTo($badgeContainer);

						if (typeof option.color === 'string') {
							kanban_sign.css({ color: option.color });
						}
					});

					if ($badgeContainer.children().length > 0) {
						const $body = $('<div class="mm-node-body"></div>').appendTo($node);
						const $layout = $('<div class="mm-node-layout"></div>').appendTo($body);
						$node.find('.mm-node-title, .mm-node-dates').appendTo($layout);
						$body.prepend($badgeContainer);
					}
				}
			}
		})();
		
		(this.makeMarkers = () => {
			var $markersBlock = false;
			function markersBlock() {
				if ($markersBlock === false) {
					$markersBlock = $('<div>', {class: 'mm-node-markers'}).appendTo($node);
					if (node.direction < 0) {
						$markersBlock.addClass('right-align');
					}
				}
				return $markersBlock;
			}
			function openURI(uri) {
				try { window.Backend.openURI(uri); } catch (e) {}
			}
			if (typeof(meta.note) == 'string') {
				$('<a>', {
					'title': MapUiConfig.getText('note_icon_caption'),
					'href': '#'
				}).noteEditor().append($('<span>', {
									'class': MapUiConfig.icons['inspector_note'] || 'mif-file-text'
								})).appendTo(markersBlock());
			}
			if (typeof (meta['file-URI']) == 'string') {
				$('<a>', {
					'title': meta['file-URI'],
					'href': '#'
				}).on('click', openURI.bind(this, meta['file-URI']))
								.append($('<span>', {
									'class': 'mif-attachment'
								})).appendTo(markersBlock());
			}
			if ( $.isArray(meta['file-URI']) ) {
				$('<a>', {
					'title': meta['file-URI'][0],
					'href': '#'
				}).csl74Menu({
					customClass: 'rolldown-menu',
					items:meta['file-URI'],
					onSelect: function (value, item, $trigger) {
						openURI(value);
					}
				}).append($('<span>', {
									'class': 'mif-attachment'
								})).appendTo(markersBlock());
			}
			if (typeof(meta.url) == 'string') {
				$('<a>', {
					'title': meta['url'],
					'href': '#'
				}).on('click', openURI.bind(this, meta['url']))
								.append($('<span>', {
									'class': 'mif-link'
								})).appendTo(markersBlock());
			}
			if ( $.isArray(meta['url']) ) {
				$('<a>', {
					'title': meta['url'][0],
					'href': '#'
				}).csl74Menu({
					customClass: 'rolldown-menu',
					items:meta['url'],
					onSelect: function (value, item, $trigger) {
						openURI(value);
					}
				}).append($('<span>', {
									'class': 'mif-link'
								})).appendTo(markersBlock());
			}
		})();

	}
	
	showInspector($container) {
		this.$container = $container;
		this.$container.empty();

		const $fieldsContainer = $('<div>', { 'class': 'mm-inspector-fields' }).appendTo(this.$container);

		$('<input>', {
			'type': 'text',
			'class': 'mm-inspector-input grow',
			'placeholder': MapUiConfig.getText('prompt_title'),
			'title': MapUiConfig.getText('prompt_title'),
			'value': this.buffer.topic,
			'on': {
				'input': (e) => {
					this.buffer.topic = $(e.target).val();
					this.updateStatesUI();
				}
			}
		}).appendTo($fieldsContainer);
		
		this.$typeForm = $('<select>', {
			'class': 'mm-select-type mm-inspector-input',
			'placeholder': MapUiConfig.getText('prompt_type'),
			'title': MapUiConfig.getText('prompt_type'),
			'on': {
				'change': (e) => this.changeNodeTypeAction($(e.target).val())
			}
		}).appendTo($fieldsContainer);

		const types = ['default', 'group', 'module', 'task', 'milestone', 'idea', 'note', 'document'];
		types.forEach(t => {
			$('<option>', {
				'value': t,
				'text': MapUiConfig.getText(`type_${t}`),
				'selected': t === this.buffer.type
			}).appendTo(this.$typeForm);
		});

		this.$detailedContainer = $('<div>', { 'class': 'mm-inspector-detailed' }).appendTo($fieldsContainer);
		this.renderDetailedFields(this.$detailedContainer);

		this.$toolbar = $('<div>', { 'class': 'mm-inspector-toolbar mm-toolbar' });

		this.$btnApply = this.$toolbar.mapAddToolButton('inspector_apply', () => this.applyChangesAction());
		this.$btnCancel = this.$toolbar.mapAddToolButton('inspector_cancel', () => this.cancelChangesAction());
		if (!this.node.isroot) {
			this.$btnNoteEdit = this.$toolbar.mapAddToolButton('inspector_fields').fieldsEditor();
		}
		
		//уже не надо
		//this.$btnNoteEdit = this.$toolbar.mapAddToolButton('inspector_note').noteEditor();
		/* передумал расширенный режим делать
		if (this.extendModeAble) {
			this.$btnToggle = this.$toolbar.mapAddToolButton('inspector_toggle_full', () => {
				this.isDetailed = !this.isDetailed;
				this.updateStatesUI();
			});
		}*/

		this.$toolbar.appendTo(this.$container);
		this.updateStatesUI();
	}

	renderDetailedFields($fieldsContainer) {}
	
	isChanged(){
		return this.buffer.topic !== this.node.topic || 
			JSON.stringify(this.buffer.meta) !== JSON.stringify(this.node.data?.data || {});
	}

	updateStatesUI() {
		const isChanged = this.isChanged();

		// Управляем видимостью блоков без пересборки DOM
		this.$typeForm.toggle(!this.node.isroot);
		this.$typeForm.prop('disabled', this.node.isroot);
		this.$detailedContainer.toggle(this.isDetailed);

		/*if (this.extendModeAble) {
			const toggleKey = this.isDetailed ? 'inspector_toggle_short' : 'inspector_toggle_full';
			//this.$btnToggle.attr('title', MapUiConfig.getText(toggleKey));
			this.$btnToggle.attr('title', crazy74.lang.get('mindmap', 'button', toggleKey));
			this.$btnToggle.find('.icon').attr('class', 'icon ' + MapUiConfig.icons[toggleKey]);
		}*/

		// Включаем / выключаем кнопки сохранения
		this.$btnApply.prop('disabled', !isChanged);
		this.$btnCancel.prop('disabled', !isChanged);
	}

	changeNodeTypeAction(newType) {
		this.buffer.type = newType;
		this.buffer.meta.type = newType;

		const ViewClass = window.App.NodeViewFactory.getClass(newType);
		const newView = new ViewClass(this.node, this.jm, this.buffer);
		newView.isDetailed = this.isDetailed;

		window.App.ProjectMapManager.currentInspectorView = newView;
		newView.showInspector(this.$container);
	}

	applyChangesAction() {
		if (this.node.topic !== this.buffer.topic) {
			this.jm.update_node(this.node.id, this.buffer.topic);
		}

		this.node.data = this.node.data || {};
		this.node.data.data = $.extend(true, {}, this.buffer.meta);

		window.App.ProjectMapManager.isDirty = true;
		
		const nodeElement = $(`jmnode[nodeid="${this.node.id}"]`);
		if (nodeElement.exists()) {
			nodeElement.className = 'jmnode'; 
			const ViewClass = window.App.NodeViewFactory.getClass(this.buffer.type);
			ViewClass.decorate(this.node, nodeElement[0]);
			if (this.node._data && this.node._data.view) {
				this.node._data.view.width = null;
				this.node._data.view.height = null;
			}
			setTimeout(() => {
				this.jm.layout.cache_valid = false;
				this.jm.view.init_nodes_size();
				this.jm.resize();
			}, 50);
			nodeElement.trigger('node:changed', [this.node]);
		}

		this.updateStatesUI();
	}

	cancelChangesAction() {
		this.buffer = {
			topic: this.node.topic,
			type: this.node.data?.data?.type || 'default',
			meta: $.extend(true, {}, this.node.data?.data || {})
		};
		this.buffer.meta.type = this.buffer.type;
		this.showInspector(this.$container);
	}
}

class RootNodeView extends BaseNodeView {
	constructor(node, jm, existingBuffer = null) {
		super(node, jm, existingBuffer);
		this.extendModeAble = true;
	}
}
class ModuleNodeView extends BaseNodeView {}
class IdeaNodeView extends BaseNodeView {}
class NoteNodeView extends BaseNodeView {}
class DocumentNodeView extends BaseNodeView {}
class GroupNodeView extends BaseNodeView {}
class MilestoneNodeView extends BaseNodeView {}

	class TaskNodeView extends BaseNodeView {
	constructor(node, jm, existingBuffer = null) {
		super(node, jm, existingBuffer);
		this.extendModeAble = true;
	}
}

window.App.NodeViewFactory = class {
	static getClass(type) {
		switch (type) {
			case 'root':     return RootNodeView;
			case 'task':     return TaskNodeView;
			case 'module':   return ModuleNodeView;
			case 'idea':     return IdeaNodeView;
			case 'note':     return NoteNodeView;
			case 'document': return DocumentNodeView;
			case 'milestone': return MilestoneNodeView;
			case 'group':		 return GroupNodeView;
			default:         return BaseNodeView;
		}
	}
};