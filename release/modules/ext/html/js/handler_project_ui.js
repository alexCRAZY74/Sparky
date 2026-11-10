window.Lang = {};
const MapUiConfig = {
	icons: {
		add_node: 'mif-plus',
		remove_node: 'mif-cross',
		save_map: 'mif-floppy-disk',
		inspector_toggle_short: 'fa fa-compress', // сворачивание полей
		inspector_toggle_full: 'fa fa-expand',    // расширение полей
		inspector_apply: 'fa fa-check',           // галочка "применить"
		inspector_cancel: 'fa fa-times',           // крестик "отмена"
		inspector_note: 'mif-file-text',
		inspector_fields: 'mif-menu',
		inspector_edit_date: 'csl74-icon icon-date',
		inspector_add_custom: 'mif-tag',
		inspector_add_url: 'mif-link',
		inspector_add_link: 'mif-attachment',
		inspector_add_file: 'mif-attachment',
		inspector_add_folder: 'mif-folder',
		type_group: 'mif-widgets',
		type_milestone: 'mif-flag',
    modal_alert: 'fa fa-exclamation-triangle',
    modal_confirm: 'fa fa-question-circle',
    modal_prompt: 'fa fa-pencil'
	},

	getText(key) {
		return crazy74.lang.get('mindmap', key);
	}
};

jQuery.fn.mapAddToolButton = function (...args) {
	var btnType, clickFn, withLabel;
	args.forEach(arg => {
		switch (typeof arg) {
			case 'string':   btnType = arg; break;
			case 'function': clickFn = arg; break;
			case 'boolean':  withLabel = arg; break;
		}
	});
	//const text = MapUiConfig.getText(btnType);
	const text = crazy74.lang.get('mindmap', 'button', btnType);
	//crazy74.array.set(window.Lang, ['mindmap', 'button', btnType], text);
	const iconClass = MapUiConfig.icons[btnType] || '';

	const $button = $('<button>', {
		'type': 'button',
		'class': `mm-tool-button mm-btn-${btnType}`,
		'title': text
	}).appendTo(this);

	if (iconClass) {
		$('<span>', {'class': 'icon ' + iconClass}).appendTo($button);
		if (withLabel === true) {
			$('<span>', {class:'label'}).text(text).appendTo($button);
		}
	} else {
		$button.text(text);
	}

	if (typeof clickFn === 'function') {
		$button.on('click', clickFn);
	}

	return $button;
};

jQuery.fn.AddConfirm = function (...args) {
	var messageCode, onConfirmFn, dialogOptions;
	args.forEach(arg => {
		switch (typeof arg) {
			case 'string':   messageCode = arg; break;
			case 'function': onConfirmFn = arg; break;
			case 'object':   dialogOptions = arg; break;
		}
	});

	const $btn = this;
	dialogOptions = dialogOptions || {};

	$btn.on('click', function (e) {
		e.preventDefault();
		e.stopPropagation();

		let isConfirmed = false;
		const message = MapUiConfig.getText(messageCode) || messageCode;
		
		const winOpt = $.extend({},true, {
			icon: MapUiConfig.icons['modal_confirm'] || false,
			title: MapUiConfig.getText('modal_confirm_title'),
			content: message,
			destroyOnClose: true,
			onClose: () => {
				if (isConfirmed && typeof onConfirmFn === 'function') {
					onConfirmFn();
				}
			}
		}, dialogOptions);

		const win = $.csl74Window(winOpt);

		const $footer = $('<div>', { class: 'csl-dialog-footer' }).insertAfter(win.$content);
		$footer.mapAddToolButton('btn_cancel', () => win.close());
		$footer.mapAddToolButton('btn_ok', () => {
			isConfirmed = true;
			win.close();
		});
	});

	return $btn;
};

$.textEditorWindow = function (options = {}) {
	const initialText = options.text || options.content || '';
	const winTitle = options.title || MapUiConfig.getText('note_editor_title') || 'Заметка';

	const $textarea = $('<textarea>', {
		class: 'csl-dialog-textarea',
		val: initialText
	});

	//const $body = $('<div>', { class: 'csl-dialog-text-editor' }).append($textarea);

	const win = $.csl74Window($.extend({}, true, options, {
		//customClass: 'csl-dialog-text-editor',
		title: winTitle,
		content: $textarea,
		withBackDrop: true,
		destroyOnClose: true
	}));
	win.$frame.addClass('csl-dialog-text-editor');

	const $footer = $('<div>', { class: 'csl-dialog-footer' }).insertAfter(win.$content);

	$footer.mapAddToolButton('inspector_cancel', () => win.close());
	$footer.mapAddToolButton('inspector_apply', () => {
		if (typeof options.onApplyChanges === 'function') {
			options.onApplyChanges($textarea.val());
		}
		win.close();
	});

	setTimeout(() => $textarea.focus(), 50);
	console.info('win', win);
	return win;
};

jQuery.fn.noteEditor = function () {
	return this.each(function () {
		const $icon = $(this);

		$icon.on('click', function (e) {
			const pm = window.App.ProjectMapManager;
			var nodeView = pm ? pm.currentInspectorView : null;
			e.preventDefault();
			e.stopPropagation();

			// Забираем текущий текст заметки из буфера или метаданных узла
			const currentNote = nodeView.buffer?.meta?.note || nodeView.node?.data?.data?.note || '';
			const currentTopic = nodeView.buffer?.topic || nodeView.node?.topic || MapUiConfig.getText('note_editor_title') || 'Заметка';

			$.textEditorWindow({
				icon: 'mif-file-text',
				title: currentTopic,
				text: currentNote,
        withBackDrop: false,
				position: [{
					my: 'left top',
					at: 'left bottom',
					of: $icon
				},{ collision: 'flipfit', within: window }],
				onApplyChanges: (newNoteText) => {
					const isChanged = nodeView.isChanged();
					console.clear();
					console.log('nodeView', nodeView);
					console.log('isChanged', isChanged);
					console.log('typeof nodeView.applyChangesAction', typeof nodeView.applyChangesAction);
					const trimmed = newNoteText.trim();
					
					if (trimmed) {
						nodeView.buffer.meta.note = trimmed;
					} else {
						delete nodeView.buffer.meta.note;
					}

					// Применяем изменения в узел и перерисовываем
					if (!isChanged && typeof nodeView.applyChangesAction === 'function') {
						nodeView.applyChangesAction();
					} else {
						nodeView.updateStatesUI();
					}
				}
			}).$frame.addClass('mm-node-note-editor');
		});
	});
};

$.fn.fieldKanbanEditor = function (code, nodeView) {
	const rootNode = window?.App?.ProjectMapManager?.jm?.get_root();
	if (!rootNode || !nodeView) return;

	let selectedKanban = false;
	const kanban_settings = rootNode?.data?.data?.kanban_settings;
	if (kanban_settings && Array.isArray(kanban_settings.lists)) {
		selectedKanban = kanban_settings.lists.find(kb => kb.code === code);
	}
	if (!selectedKanban) return;
	const unselectValue = '~unselect~';
	// Ищем текущее значение селектора в метаданных узла
	const currentKanbans = nodeView.buffer?.meta?.kanbans || nodeView.node?.data?.data?.kanbans || [];
	const currentKbItem = Array.isArray(currentKanbans) ? currentKanbans.find(k => k.code === code) : null;
	const currentValue = currentKbItem ? currentKbItem.value : unselectValue;

	const $block = $('<div>', { class: 'form-group mm-kanban-field' }).appendTo(this);
	$('<label>', { class: 'mm-kanban-label' }).text(selectedKanban.title).appendTo($block);

	const $selectInput =$('<select>', {
		type: 'text',
		class: 'form-control',
		name: `kanban_${code}`,
	}).appendTo($block);
	$('<option>').text(currentValue).appendTo($selectInput);
	$selectInput.val(currentValue);

	// Подготавливаем элементы для csl74Select
	let selectOptions = (selectedKanban.options || []).map(opt => ({
		value: opt.value,
		label: opt.text,
		icon: opt.icon,
		color: opt.color
	}));
	selectOptions.push('-');
	selectOptions.push({value:unselectValue, label: crazy74.lang.get('mindmap', 'kanban_unselect')});

	$selectInput.csl74Select({
		items: selectOptions,
		onSelect: function (val, selitem) {
			if (!nodeView.buffer.meta) {
				nodeView.buffer.meta = {};
			}
			if (!Array.isArray(nodeView.buffer.meta.kanbans)) {
				// Клонируем исходный массив kanbans, если ещё не клонировали
				const origKanbans = nodeView.node?.data?.data?.kanbans || [];
				nodeView.buffer.meta.kanbans = $.extend(true, [], origKanbans);
			}

			let item = nodeView.buffer.meta.kanbans.find(k => k.code === code);
			if (item) {
				item.value = val;
			} else {
				nodeView.buffer.meta.kanbans.push({ code: code, value: val });
			}

			if (typeof nodeView.updateStatesUI === 'function') {
				nodeView.updateStatesUI();
			}
		}
	});
	return $block;
};

$.fn.fieldLinksEditor = function (nodeView, editor) {
	if (!nodeView || !nodeView.buffer || !nodeView.buffer.meta) return this;

	const $container = this;
	$container.empty();

	const meta = nodeView.buffer.meta;
	const openURI = (uri) => {
		try {
			if (window.Backend && typeof window.Backend.openURI === 'function') {
				window.Backend.openURI(uri);
			}
		} catch (e) {}
	};

	const removeItem = async (key, index) => {
		const removeConfirm = await confirm(
						crazy74.lang.get('mindmap', 'dataedit', 'remove_link_confirm')
						,crazy74.lang.get('mindmap', 'dataedit', 'remove_dialog'));
		if (!removeConfirm) {
			return;
		}
		const val = meta[key];
		if (Array.isArray(val)) {
			val.splice(index, 1);
			if (val.length === 0) {
				delete meta[key];
			} else if (val.length === 1 && typeof nodeView.node?.data?.data?.[key] === 'string') {
				meta[key] = val[0];
			}
		} else {
			delete meta[key];
		}

		if (typeof nodeView.updateStatesUI === 'function') {
			nodeView.updateStatesUI();
		}
		if (editor && typeof editor.rebuild === 'function') {
			editor.rebuild();
		}
	};

	const renderSection = (key, labelText, defaultIcon, openIcon) => {
		const rawVal = meta[key];
		if (typeof rawVal === 'undefined') return;

		const items = Array.isArray(rawVal) ? rawVal : [rawVal];
		if (items.length === 0) return;

		const $section = $('<div>', { class: 'form-group mm-links-section' }).appendTo($container);
		$('<label>', { class: 'mm-field-label', text: labelText }).appendTo($section);

		const $list = $('<div>', { class: 'mm-links-list' }).appendTo($section);

		items.forEach((item, index) => {
			const $itemRow = $('<div>', { class: 'control-group mm-link-item' }).appendTo($list);
			$('<span>', { class: defaultIcon })
							.appendTo($('<span>', { class: 'form-control icon' }).appendTo($itemRow));
			/*$('<input>', {
				type: 'text',
				class: 'form-control',
				readonly: true,
				value: item
			}).appendTo($itemRow);*/
			$('<span>', {
				class: 'form-control label',
				title: item,
				text: crazy74.string.strcut(item, 40)
			}).appendTo($itemRow);

			const $btnOpen = $('<button>', {
				type: 'button',
				class: 'form-control speed-button btn-open-link',
				title: crazy74.lang.get('mindmap', 'button', 'open') || 'Открыть'
			}).on('click', () => openURI(item)).appendTo($itemRow);
			$('<span>', { class: openIcon }).appendTo($btnOpen);

			const $btnDelete = $('<button>', {
				type: 'button',
				class: 'form-control speed-button btn-delete-link',
				title: crazy74.lang.get('mindmap', 'button', 'remove') || 'Удалить'
			}).on('click', () => removeItem(key, index)).appendTo($itemRow);
			$('<span>', { class: MapUiConfig.icons.remove_node || 'mif-cross' }).appendTo($btnDelete);
		});
	};

	renderSection('file-URI', crazy74.lang.get('mindmap', 'dataedit', 'tabs', 'files') || 'Файлы', 'mif-attachment', 'mif-folder-open');
	renderSection('url', crazy74.lang.get('mindmap', 'dataedit', 'tabs', 'links') || 'Ссылки', 'mif-link', 'fa fa-share-square');

	return $container;
};

$.fn.fieldCustomEditor = function (fieldKey, nodeView) {
	const rawVal = nodeView.buffer?.meta?.[fieldKey] ?? nodeView.node?.data?.data?.[fieldKey] ?? '';
	const valStr = typeof rawVal === 'object' ? JSON.stringify(rawVal) : String(rawVal);

	const $block = $('<div>', { class: 'form-group mm-custom-field' }).appendTo(this);
	$('<label>', { class: 'mm-custom-field-label' }).text(fieldKey).appendTo($block);

	const $input = $('<input>', {
		type: 'text',
		class: 'form-control',
		name: `field_${fieldKey}`,
		val: valStr
	}).appendTo($block);

	$input.on('input change', function () {
		if (!nodeView.buffer.meta) {
			nodeView.buffer.meta = {};
		}
		const val = $(this).val();
		if (val.trim() !== '') {
			nodeView.buffer.meta[fieldKey] = val;
		} else {
			delete nodeView.buffer.meta[fieldKey];
		}
		if (typeof nodeView.updateStatesUI === 'function') {
			nodeView.updateStatesUI();
		}
	});

	return $block;
};

const addFieldURLDialog = async function(editor){
	const pm = window.App?.ProjectMapManager;
	const nodeView = editor?.nodeView || pm?.currentInspectorView;
	if (!nodeView) {
		return;
	}
	
	const title = crazy74.lang.get('mindmap', 'dataedit', 'enter_url') || 'URL адрес';
	const urlVal = await prompt(title, 'https://');

	if (!urlVal || !urlVal.trim()) {
		return;
	}

	const cleanUrl = urlVal.trim();

	if (!nodeView.buffer.meta) {
		nodeView.buffer.meta = {};
	}

	const existingUrl = nodeView.buffer.meta.url ?? nodeView.node?.data?.data?.url;

	if (typeof existingUrl === 'undefined') {
		nodeView.buffer.meta.url = cleanUrl;
	} else if (Array.isArray(existingUrl)) {
		nodeView.buffer.meta.url = [...existingUrl, cleanUrl];
	} else {
		nodeView.buffer.meta.url = [existingUrl, cleanUrl];
	}

	if (typeof nodeView.updateStatesUI === 'function') {
		nodeView.updateStatesUI();
	}

	if (editor && typeof editor.rebuild === 'function') {
		editor.rebuild('url');
	}
};

const FieldType = Object.freeze({
    STRING: 'string',
    DATE: 'date',
    DATETIME: 'datetime',
    DATE_RANGE: 'date-range'
});

const addFieldDialog = function(TypeOffield, editor){
	const pm = window.App?.ProjectMapManager;
	const nodeView = editor?.nodeView || pm?.currentInspectorView;
	if (!nodeView) {
		return;
	}
	
	const formGroup = function (labelKey, buttonIcon, name = 'new_field_name') {
		const $block = $('<div>', {class: 'form-group'});
		if (typeof(labelKey) === 'string') {
			$('<label>', { class: 'mm-field-label', text: crazy74.lang.get('mindmap', 'dataedit', labelKey)}).appendTo($block);
		}
		const $input = $('<input>', {
			type: 'text',
			class: 'form-control',
			name: name
		}).appendTo($block);
		if (typeof(buttonIcon) === 'string') {
			$input.wrap($('<div>', {class: 'control-group'}));
			$('<span>', {class: buttonIcon})
							.appendTo($('<button>', {type: 'button', class: 'speed-button'}).insertAfter($input));
		}
		return $block;
	};

	const $body = $('<div>', { class: 'form-group mm-add-field-dialog' });

	let winTitle = crazy74.lang.get('mindmap', 'dataedit', 'add_string') || 'Добавить строковое поле';
	let winIcon = MapUiConfig.icons.inspector_add_custom || 'mif-tag';

	if (TypeOffield === FieldType.DATE || TypeOffield === FieldType.DATETIME || TypeOffield === FieldType.DATE_RANGE) {
		winIcon = MapUiConfig.icons.inspector_edit_date || 'menu-icon icon-date';
		if (TypeOffield === FieldType.DATE) {
			winTitle = crazy74.lang.get('mindmap', 'dataedit', 'add_date') || 'Добавить дату';
		} else if (TypeOffield === FieldType.DATETIME) {
			winTitle = crazy74.lang.get('mindmap', 'dataedit', 'add_datetime') || 'Добавить дату и время';
		} else if (TypeOffield === FieldType.DATE_RANGE) {
			winTitle = crazy74.lang.get('mindmap', 'dataedit', 'add_date_start_end') || 'Добавить период дат';
		}
	}
 
	const win = $.csl74Window({
		customClass: 'add-field-dialog',
		icon: winIcon,
		title: winTitle,
		content: $body,
		withBackDrop: true,
		destroyOnClose: true
	});
	win.$frame.addClass('add-field-dialog');

	let onApply;
	const toIso = (v) => {
		if (!v) return v;
		const d = new Date(v);
		if (isNaN(d.getTime())) return v;
		return crazy74?.date?.toIso ? crazy74.date.toIso(v) : d.toISOString();
	};

	switch (TypeOffield) {
		case FieldType.STRING:
			(function addStringField() {
				const $nameInput = formGroup('field_name', null, 'field_name').appendTo($body).find('input');
				const $valInput = formGroup('field_value', null, 'field_value').appendTo($body).find('input');

				onApply = function () {
					const key = $nameInput.val().trim();
					if (!key) {
						$nameInput.focus();
						return;
					}

					const reservedKeys = new Set(['id', 'parentid', 'isroot', 'kanbans', 'note', 'type', 'sys', 'bx', 'path', 'ref', 'kanban_settings', 'date', 'datetime', 'dates']);
					if (reservedKeys.has(key)) {
						MapDialogs.alert(crazy74.lang.get('mindmap', 'dataedit', 'reserved_field_name') || 'Имя поля зарезервировано');
						return;
					}

					if (!nodeView.buffer.meta) {
						nodeView.buffer.meta = {};
					}

					nodeView.buffer.meta[key] = $valInput.val();

					if (typeof nodeView.updateStatesUI === 'function') {
						nodeView.updateStatesUI();
					}

					win.close();

					if (editor && typeof editor.rebuild === 'function') {
						editor.rebuild(key);
					}
				};

				$nameInput.add($valInput).on('keydown', function (e) {
					if (e.key === 'Enter') {
						onApply();
					}
				});

				setTimeout(() => $nameInput.focus(), 50);
			})();
			break;

		case FieldType.DATE:
		case FieldType.DATETIME:
			(function addSingleDateField() {
				const isTime = (TypeOffield === FieldType.DATETIME);
				const key = isTime ? 'datetime' : 'date';
				const existingVal = nodeView.buffer?.meta?.[key] ?? nodeView.node?.data?.data?.[key] ?? '';

				const labelKey = isTime ? 'field_datetime' : 'field_date';
				const $group = formGroup(labelKey, 'csl74-icon csl74-icon-'+key, key).appendTo($body);
				const $input = $group.find('input');
				if (existingVal) {
					$input.val(existingVal);
				}
				$group.find('.control-group').csl74DatePicker({
					pickTime: isTime
				});

				onApply = function () {
					const val = $input.val().trim();
					console.log('val', val, crazy74.date.parseDate(val));

					if (!nodeView.buffer.meta) {
						nodeView.buffer.meta = {};
					}

					if (val) {
						nodeView.buffer.meta[key] = toIso(val);
					} else {
						delete nodeView.buffer.meta[key];
					}

					if (typeof nodeView.updateStatesUI === 'function') {
						nodeView.updateStatesUI();
					}

					win.close();

					if (editor && typeof editor.rebuild === 'function') {
						editor.rebuild(key);
					}
				};

				setTimeout(() => $input.focus(), 50);
			})();
			break;

		case FieldType.DATE_RANGE:
			(function addDateRangeField() {
				const existingDates = nodeView.buffer?.meta?.dates || nodeView.node?.data?.data?.dates || {};

				const $startGroup = formGroup('field_date_start', 'csl74-icon csl74-icon-datetime', 'date_start').appendTo($body);
				const $startInput = $startGroup.find('input');
				if (existingDates.start) {
					$startInput.val(existingDates.start);
				}
				$startGroup.find('.control-group').csl74DatePicker({
					timePicker: false
				});

				const $endGroup = formGroup('field_date_end', 'csl74-icon csl74-icon-datetime', 'date_end').appendTo($body);
				const $endInput = $endGroup.find('input');
				if (existingDates.end) {
					$endInput.val(existingDates.end);
				}
				$endGroup.find('.control-group').csl74DatePicker({
					timePicker: false
				});

				onApply = function () {
					const startVal = $startInput.val().trim();
					const endVal = $endInput.val().trim();

					if (!nodeView.buffer.meta) {
						nodeView.buffer.meta = {};
					}

					if (!startVal && !endVal) {
						delete nodeView.buffer.meta.dates;
					} else {
						const datesObj = {};
						if (startVal) datesObj.start = toIso(startVal);
						if (endVal) datesObj.end = toIso(endVal);
						nodeView.buffer.meta.dates = datesObj;
						//nodeView.buffer.meta.dates = JSON.stringify(datesObj);
					}

					if (typeof nodeView.updateStatesUI === 'function') {
						nodeView.updateStatesUI();
					}

					win.close();

					if (editor && typeof editor.rebuild === 'function') {
						editor.rebuild('dates');
					}
				};

				setTimeout(() => $startInput.focus(), 50);
			})();
			break;
	}

	const $footer = $('<div>', { class: 'csl-dialog-footer' }).insertAfter(win.$content);
	$footer.mapAddToolButton('inspector_cancel', () => win.close());
	$footer.mapAddToolButton('inspector_apply', onApply);
};

$.fn.fieldsEditor = function () {
	var $icon;
	
	const DateField = function(options){
		function DateFieldHadnler(options){
			const self = this;
			const opts = $.extend(true, {
				fieldKey: 'date',
				labelKey: 'date',
				pickTime: true,
				value: '',
				onChange: false
			}, options);
			this.fieldKey = opts.fieldKey;
			this.$block = $('<div>', {class: 'form-group'});
			$('<label>', { class: 'mm-field-label', text: crazy74.lang.get('mindmap', 'dataedit', opts.labelKey)}).appendTo(this.$block);
			const $group = $('<div>', {class: 'control-group'}).appendTo(this.$block);
			this.$input = $('<input>', {
				type: 'text',
				class: 'form-control',
				value: opts.value
			}).appendTo($group);
			const buttonIcon = opts.pickTime ? 'csl74-icon csl74-icon-datetime' : 'csl74-icon csl74-icon-date';
			$('<span>', {class: buttonIcon})
							.appendTo($('<button>', {type: 'button', class: 'speed-button'})
							.insertAfter(this.$input));
			$group.csl74DatePicker({
				pickTime: opts.pickTime
			});

			this.$input.on('input change', function () {
				if (typeof opts.onChange === 'function') {
					opts.onChange.call(self);
				}
			});
		}
		DateFieldHadnler.prototype.appendTo = function ($object){
			this.$block.appendTo($object);
			return this;
		};
		DateFieldHadnler.prototype.value = function (){
			const raw = this.$input.val() ? this.$input.val().trim() : '';
			if (!raw) return false;
			const d = new Date(raw);
			if (isNaN(d.getTime())) return false;
			return crazy74?.date?.toIso ? crazy74.date.toIso(raw) : d.toISOString();
		};
		DateFieldHadnler.prototype.assign = function (object){
			if (!object || typeof object !== 'object') return;
			const val = this.value();
			if (val === false) {
				delete object[this.fieldKey];
			} else {
				object[this.fieldKey] = val;
			}
		};
		return new DateFieldHadnler(options);
	};
		
	const Editor = function (e) {
		const that = this;
		const pm = window.App.ProjectMapManager;
		var nodeView = pm ? pm.currentInspectorView : null;
		const rootNode = window.App.ProjectMapManager?.jm?.get_root();
		e.preventDefault();
		e.stopPropagation();

		const currentTopic = nodeView.buffer?.topic || nodeView.node?.topic || MapUiConfig.getText('fields_editor_title') || 'Данные';

		const win = $.csl74Window({
			icon: 'mif-menu',
			title: currentTopic,
			withBackDrop: true,
			position: [[
					{
						my: 'right top',
						at: 'right bottom',
						of: $icon
					},
					{
						my: 'bottom',
						at: 'top',
						of: '#bottom-panel'
					}
				], {collision: 'flipfit', within: window}],
		});

		const $fieldspanel = $('<div>', { class: 'fields-editor-fields' });
		const tabs = $fieldspanel.csl74Tabs({
			onSelect: function(){
				win.realign();
			}
		});
		const $tabFields = tabs.addTab(crazy74.lang.get('mindmap', 'dataedit', 'tabs', 'fields'));
		const $tabFieldsLayout = $('<div>',{class: 'field-set'}).appendTo($tabFields);
		var $tabLinks = false;

		const rebuild = this.rebuild = function (focusFieldName) {
			console.info('rebuild');
			console.log('data', nodeView?.node?.data?.data);
			console.log('buffer', nodeView?.buffer?.meta);
			$tabFieldsLayout.empty();

			const kanban_settings = rootNode?.data?.data?.kanban_settings;
			if (kanban_settings && Array.isArray(kanban_settings.lists)) {
				const grid = $('<div>', { class: 'fields-grid' }).appendTo($tabFieldsLayout);
				kanban_settings.lists.forEach(kb => {
					grid.fieldKanbanEditor(kb.code, nodeView);
				});
			}
			
			if (typeof(nodeView?.buffer?.meta['file-URI']) !== 'undefined' || typeof(nodeView?.buffer?.meta['url']) !== 'undefined') {
				if (!$tabLinks) {
					$tabLinks = tabs.addTab(crazy74.lang.get('mindmap', 'dataedit', 'tabs', 'links'));
				}
				$tabLinks.empty();
				$tabLinks.fieldLinksEditor(nodeView, that);
			} else if ($tabLinks !== false) {
				$tabLinks.empty();
			}
			if (typeof(nodeView?.buffer?.meta?.date) !== 'undefined') {
				console.log('date', nodeView?.buffer?.meta?.date, typeof(nodeView?.buffer?.meta?.date));
				DateField({
					fieldKey: 'date',
					labelKey: 'field_date',
					pickTime: false,
					value: nodeView?.buffer?.meta?.date,
					onChange: function(){
						this.assign(nodeView.buffer.meta);
					}
				}).appendTo($tabFieldsLayout);
			}
			if (typeof(nodeView?.buffer?.meta?.datetime) !== 'undefined') {
				console.log('datetime', nodeView?.buffer?.meta?.datetime, typeof(nodeView?.buffer?.meta?.datetime));
				DateField({
					fieldKey: 'datetime',
					labelKey: 'field_datetime',
					pickTime: true,
					value: nodeView?.buffer?.meta?.datetime,
					onChange: function(){
						this.assign(nodeView.buffer.meta);
					}
				}).appendTo($tabFieldsLayout);
			}
			if (typeof(nodeView?.buffer?.meta?.dates) !== 'undefined') {
				var currentDates = nodeView?.buffer?.meta?.dates || {};
				console.log('currentDates', currentDates, typeof(currentDates));
				const $datesBlock = $('<div>', {class:'fields-grid'}).appendTo($tabFieldsLayout);
				const updateDatesMeta = () => {
					if (!nodeView.buffer.meta) {
						nodeView.buffer.meta = {};
					}
					if (Object.keys(currentDates).length === 0) {
						delete nodeView.buffer.meta.dates;
					} else {
						nodeView.buffer.meta.dates = currentDates;
					}
				};

				DateField({
					fieldKey: 'start',
					labelKey: 'field_date_start',
					pickTime: true,
					value: currentDates?.start || '',
					onChange: function(){
						this.assign(currentDates);
						updateDatesMeta();
					}
				}).appendTo($datesBlock);

				DateField({
					fieldKey: 'end',
					labelKey: 'field_date_end',
					pickTime: true,
					value: currentDates?.end || '',
					onChange: function(){
						this.assign(currentDates);
						updateDatesMeta();
					}
				}).appendTo($datesBlock);
			}

		const handledKeys = new Set(['kanbans', 'note', 'type', 'date', 'datetime', 'dates', 'sys', 'bx', 'path', 'ref', 'kanban_settings', 'url', 'file-URI']);
			const metaKeys = new Set([
				...Object.keys(nodeView?.node?.data?.data || {}),
				...Object.keys(nodeView?.buffer?.meta || {})
			]);

			metaKeys.forEach(key => {
				if (!handledKeys.has(key)) {
					$tabFieldsLayout.fieldCustomEditor(key, nodeView);
				}
			});

			setTimeout(() => {
				win.realign();
				if (focusFieldName) {
					const $target = win.$content.find(`[name="field_${focusFieldName}"], [name="kanban_${focusFieldName}"], [name="${focusFieldName}"]`).first();
					if ($target.length) {
						$target.focus();
					}
				}
			}, 10);
		};

		that.win = win;
		win.rebuild = rebuild;
		win.repaint = rebuild;
		rebuild();

		const currentNote = nodeView.buffer?.meta?.note || nodeView.node?.data?.data?.note || '';
		const $tabNote = tabs.addTab(crazy74.lang.get('mindmap', 'dataedit', 'tabs', 'note'));
		const $textarea = $('<textarea>', {
			class: 'csl-dialog-textarea',
			val: currentNote
		}).appendTo($tabNote);

		$textarea.on('input', function () {
			if (!nodeView.buffer.meta) {
				nodeView.buffer.meta = {};
			}
			const trimmed = $(this).val().trim();
			if (trimmed) {
				nodeView.buffer.meta.note = trimmed;
			} else {
				delete nodeView.buffer.meta.note;
			}
			if (typeof nodeView.updateStatesUI === 'function') {
				nodeView.updateStatesUI();
			}
		});

		win.$frame.addClass('mm-node-fields-editor cls74-control');

		//const $footer = $('<div>', { class: 'csl-dialog-footer' }).insertAfter(win.$content);
		$fieldspanel.appendTo(win.$content);
		const $sidepanel = $('<div>', { class: 'fields-editor-sidepanel' }).appendTo(win.$content);
		const $toolsTop = $('<div>', { class: 'fields-editor-toolbar' }).appendTo($sidepanel);
		const $toolsBottom = $('<div>', { class: 'fields-editor-toolbar' }).appendTo($sidepanel);

		$toolsTop.mapAddToolButton('inspector_edit_date').csl74Menu({
			items: [
				{value: FieldType.DATE, label: crazy74.lang.get('mindmap', 'dataedit', 'add_date')},
				{value: FieldType.DATETIME, label: crazy74.lang.get('mindmap', 'dataedit', 'add_datetime')},
				{value: FieldType.DATE_RANGE, label: crazy74.lang.get('mindmap', 'dataedit', 'add_date_start_end')}
			],
			onSelect: function (value, item, $trigger) {
				addFieldDialog(value, that);
			}
		});
		$toolsTop.mapAddToolButton('inspector_add_link').csl74Menu({
			items: [
				{
					value: 'url', 
					label: crazy74.lang.get('mindmap', 'button', 'inspector_add_url'),
					icon: MapUiConfig.icons.inspector_add_url
				},
				{
					value: 'file', 
					label: crazy74.lang.get('mindmap', 'button', 'inspector_add_file'),
					icon: MapUiConfig.icons.inspector_add_file
				},
				{
					value: 'folder', 
					label: crazy74.lang.get('mindmap', 'button', 'inspector_add_folder'),
					icon: MapUiConfig.icons.inspector_add_folder
				}
			],
			onSelect: function (value, item, $trigger) {
				if (value == 'file' || value == 'folder') {
						console.log('value', value);
					window.Handler.selectFile(value,function(str){
						console.log('str', str);
						if (!nodeView.buffer.meta) {
							nodeView.buffer.meta = {};
						}
						const existingURI = nodeView?.buffer?.meta['file-URI'];
						if (Array.isArray(existingURI)) {
							nodeView.buffer.meta['file-URI'] = [...existingURI, str];
						} else {
							nodeView.buffer.meta['file-URI'] = [str];
						}
						if (typeof nodeView.updateStatesUI === 'function') {
							nodeView.updateStatesUI();
						}
						if (that && typeof that.rebuild === 'function') {
							that.rebuild('file-URI');
						}
					});
				}
				if (value == 'url') {
					addFieldURLDialog(that);
				}
			}
		});
		$toolsTop.mapAddToolButton('inspector_add_custom', () => {
			addFieldDialog(FieldType.STRING, that);
		});

		const snapshotMeta = $.extend(true, {}, nodeView?.buffer?.meta || {});

		$toolsBottom.mapAddToolButton('inspector_cancel', () => {
			if (nodeView && nodeView.buffer) {
				nodeView.buffer.meta = $.extend(true, {}, snapshotMeta);
				if (typeof nodeView.updateStatesUI === 'function') {
					nodeView.updateStatesUI();
				}
			}
			win.close();
		});
		$toolsBottom.mapAddToolButton('inspector_apply', () => {
			if (nodeView) {
				if (typeof nodeView.applyChangesAction === 'function') {
					nodeView.applyChangesAction();
				} else if (typeof nodeView.updateStatesUI === 'function') {
					nodeView.updateStatesUI();
				}
			}
			win.close();
		});

		win.realign();

	};

	return this.each(function () {
		$icon = $(this);
		$icon.on('click', (...args) => {
				new Editor(...args);
			});
	});
};


/**
 * Минималистичный модуль диалогов на базе $.cslWindow и mapAddToolButton
 */
const MapDialogs = {
	alert(message, title) {
		return new Promise((resolve) => {
			const winTitle = title || MapUiConfig.getText('modal_alert_title');
			const win = $.csl74Window({
				icon: MapUiConfig.icons['modal_alert'] || false,
				title: winTitle,
				content: message,
				destroyOnClose: true,
				onClose: () => resolve()
			});
			const $footer = $('<div>', {class: 'csl-dialog-footer'}).insertAfter(win.$content);
			$footer.mapAddToolButton('btn_ok', () => win.close());
		});
	},

	confirm(message, title) {
		return new Promise((resolve) => {
			let isConfirmed = false;
			const winTitle = title || MapUiConfig.getText('modal_confirm_title');
			const win = $.csl74Window({
				icon: MapUiConfig.icons['modal_confirm'] || false,
				title: winTitle,
				content: message,
				destroyOnClose: true,
				onClose: () => resolve(isConfirmed)
			});
			const $footer = $('<div>', {class: 'csl-dialog-footer'}).insertAfter(win.$content);
			$footer.mapAddToolButton('btn_cancel', () => win.close());
			$footer.mapAddToolButton('btn_ok', () => {
				isConfirmed = true;
				win.close();
			});
		});
	},

	prompt(message, defaultValue = '', title) {
		return new Promise((resolve) => {
			let resultValue = null;
			const winTitle = title || MapUiConfig.getText('modal_prompt_title');

			const $body = $('<div>')
							.append($('<div>', {class: 'csl-dialog-text', text: message}));

			const $input = $('<input>', {
				type: 'text',
				class: 'csl-dialog-input',
				value: defaultValue
			}).appendTo($body);

			const win = $.csl74Window({
				icon: MapUiConfig.icons['modal_prompt'] || false,
				title: winTitle,
				content: $body,
				destroyOnClose: true,
				onClose: () => resolve(resultValue)
			});

			const $footer = $('<div>', {class: 'csl-dialog-footer'}).insertAfter(win.$content);
			$footer.mapAddToolButton('btn_cancel', () => win.close());
			$footer.mapAddToolButton('btn_ok', () => {
				resultValue = $input.val();
				win.close();
			});

			setTimeout(() => $input.focus().select(), 50);
			$input.on('keydown', (e) => {
				if (e.key === 'Enter') {
					resultValue = $input.val();
					win.close();
				}
			});
		});
	}
};

// Переопределяем системные вызовы браузера
window.alert = MapDialogs.alert;
window.confirm = MapDialogs.confirm;
window.prompt = MapDialogs.prompt;