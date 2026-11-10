jQuery.fn.chatAddToolButton = function(btnType, clickFn){
	var button = $('<button>',{type: 'button'}).appendTo(this);
	if (btnType == 'copy') {
		button.html('<span class="mif-files-empty"></span>');
	}
	if (btnType == 'apply-edit') {
		button.html('<span class="mif-download"></span>');
	}
	if (btnType == 'open-file') {
		button.html('<span class="mif-folder-open"></span>');
	}
	button.attr('title', App.label('button',btnType));
	if (typeof(clickFn) == 'function') {
		button.on('click', clickFn);
	}
	return button;
};

function SysMessage(param){
	var defaultData = {
		guid: $.getGUID(),
		time: (new Date()).toLocaleString()
	};
	if (typeof (param) === 'string') {
		try {
			this.data = $.extend(true,{},defaultData,JSON.parse(param));
		} catch (err) {
			this.data = $.extend(true,{},defaultData,{text: param});
		}
	}
	if (typeof (param) === 'object') {
		this.data = $.extend(true,{},defaultData,clone(param));
	}
	function Render(){
		let className = 'system-message';
		if (typeof(this.data.error) !== 'undefined') {
			className = 'system-message error';
		}
		let block = $('<div>',{id:this.data.guid, class:className});
		$('<div>',{class:'time'}).SmartTime(this.data.time).appendTo(block);
		var msg = $('<div>',{class:'message'}).appendTo(block);
		function add(key) {
			if (typeof(this.data[key]) !== 'undefined') {
				var text = typeof(this.data[key]) === 'string' ? this.data[key] : JSON.stringify(this.data[key]);
				$('<p>', {class: 'text'}).text(text).appendTo(msg);
			}
		}
		add.call(this, 'text');
		add.call(this, 'error');
		let key = 'html';
		if (typeof(this.data[key]) !== 'undefined') {
			var text = typeof(this.data[key]) === 'string' ? this.data[key] : JSON.stringify(this.data[key]);
			$('<p>', {class: 'text'}).html(text).appendTo(msg);
		}
		return block;
	}
	this.Render = Render.bind(this);
}
function RequestHandler(param){
	console.group('RequestHandler');
	//console.log('param', param);
	var defaultData = {
		guid: $.getGUID(),
		time: new Date(),
		request: '...wait'
	};
	if (typeof (param) === 'string') {
		try {
			this.data = $.extend(true,{},defaultData,JSON.parse(param));
		} catch (err) {
			this.data = $.extend(true,{},defaultData,{request: param});
		}
	}
	if (typeof (param) === 'object') {
		this.data = $.extend(true,{},defaultData,clone(param));
	}
	if (typeof (this.data.time) === 'string') {
		try {
			this.data.time = new Date(this.data.time);
		} catch (err) {
			this.data.time = new Date();
		}
	}
	console.log('guid', this.data.guid);
	this.Render();
	console.info(this);
	console.groupEnd();
}
RequestHandler.prototype = {
	Update: function (param) {
		var incomingData = {};
		if (typeof (param) === 'string') {
			try {
				incomingData = JSON.parse(param);
			} catch (err) {
				incomingData = {request: param};
			}
		} else if (typeof (param) === 'object') {
			incomingData = clone(param);
		}

		// Если ключи были удалены на бэкенде, сбрасываем их и на фронтенде
		if (!incomingData.hasOwnProperty('error')) delete this.data.error;
		if (!incomingData.hasOwnProperty('message')) delete this.data.message;
		if (!incomingData.hasOwnProperty('interrupted')) delete this.data.interrupted;

		if (typeof (incomingData.time) === 'string') {
			try {
				incomingData.time = new Date(incomingData.time);
			} catch (err) {
				incomingData.time = new Date();
			}
		}

		this.data = $.extend(true, {}, this.data, incomingData);
		this.Render();
	},
	Update1: function (param) {
		if (typeof (param) === 'string') {
			try {
				this.data = $.extend(true, {}, this.data, JSON.parse(param));
			} catch (err) {
				this.data = $.extend(true, {}, this.data, {request: param});
			}
		}
		if (typeof (param) === 'object') {
			this.data = $.extend(true, {}, this.data, clone(param));
		}
		if (typeof (this.data.time) === 'string') {
			try {
				this.data.time = new Date(this.data.time);
			} catch (err) {
				this.data.time = new Date();
			}
		}
		this.Render();
	},
	toJson: function(){
		return JSON.stringify(this.data);
	},
	Render: function(data){
		//console.clear();
		console.group('RequestHandler.Render()');
		if (typeof(data) === 'undefined') {
			data = this.data;
		}
		var selector = '#'+data.guid;
		console.log('selector', selector);
		console.log('data', data);
		var block = $(selector);
		if (block.length <= 0) {
			block = $('<div>',{id:data.guid, class: 'question-n-answer'}).appendTo('#content');
			var req = $('<div>', {class: 'request'}).appendTo(block);
			$('<div>', {class: 'time'}).SmartTime(data.time).appendTo(req);
			$('<div>', {class: 'query'}).appendTo(req);
			//this.writeToConsole('time: '+typeof(data.time), data.time);
		}
		block.find('.request .time').SmartTime(data.time);
		( function renderQueryText() {
			//console.clear();
			let patchId = $.getGUID();
			let blockText = block.find('.request .query');
			blockText.empty();
			blockText.attr('data-patch-id', patchId);
			if (typeof(window.PatchStorage) !== 'object') {
				window.PatchStorage = {};
			}
			window.PatchStorage[patchId] = {
				text: data.request
			};
			let toolbar = $('<div>',{class:'toolbar'}).appendTo(blockText);
			$('<div>',{class:'text'}).html(escapeHtml(data.request)).appendTo(blockText);
			$('<button>',{type: 'button', class: 'toggle-expanded roll-down', title: App.label('button', 'roll-down')}).html('<span class="mif-chevron-thin-down"></span>').appendTo(toolbar);
			$('<button>',{type: 'button', class: 'toggle-expanded roll-up', title: App.label('button', 'roll-up')}).html('<span class="mif-chevron-thin-up"></span>').appendTo(toolbar);
			toolbar.on('click','.toggle-expanded', function(e){
				e.preventDefault();
				$(this).closest('.query').toggleClass('expanded');
			});
			let doCopy = function(e){
				e.preventDefault();
				//var $button = $(event.target);
				var $button = $(this);
				var $block = $button.closest('.query');
				try {
					var patchId = $block.attr('data-patch-id');
					var patchData = window.PatchStorage[patchId];		
					var codeToCopy = patchData.text;
					window.Backend.copyToClipboard(codeToCopy);
					var originalText = $button.html();
					$button.html('<span>✔</span>').css('color', '#28a745');
					setTimeout(function () {
						$button.html(originalText).css('color', '');
					}, 1000);
				} catch (e) {
					
				}

			};
			toolbar.chatAddToolButton('copy', doCopy);
		}());
		var $answer = block.find('.answer').first();
		if ($answer.length <= 0) {
			$answer = $('<div>', {class: 'answer'}).appendTo(block);
		} else {
			$answer.empty();
		}
		console.log('data.answer', data.answer);
		if (typeof(data.answer) == 'string') {
			$('<div>', {class: 'text'}).html(data.answer).appendTo($answer);
		}
		if (typeof(data.answer) == 'object') {
			(function renderAnwerText() {
				if (typeof (data.answer.time) == 'string') {
					$('<div>', {class: 'time'}).SmartTime(data.answer.time).appendTo($answer);
				}
				if (typeof (data.answer.text) == 'string') {
					var rawText = data.answer.text;
					var processedText = rawText.replace(/<file_patch\s+uri="([^"]+)"\s+start="([^"]+)"\s+end="([^"]+)">([\s\S]*?)<\/file_patch>/g, function (match, uri, start, end, patch) {
						let renderer = new RendererCodeDiffPatch(uri, start, end, patch);
						return renderer.Render();
					});
					processedText = processedText.replace(/<simple_code\s+label="([^"]+)">([\s\S]*?)<\/simple_code>/g, function (match, label, patch) {
						let renderer = new RendererCodeSimple(label, patch);
						return renderer.Render();
					});
					$('<div>', {class: 'text'}).html(processedText).appendTo($answer);
					if (typeof (window.PatchStorage) !== 'object') {
						window.PatchStorage = {};
					}
					let patchId = $.getGUID();
					window.PatchStorage[patchId] = {
						text: rawText
					};
					$answer.attr('data-patch-id', patchId);
					let toolbar = $('<div>', {class: 'toolbar'}).appendTo($answer);
					let doCopy = function (e) {
						e.preventDefault();
						//var $button = $(event.target);
						var $button = $(this);
						var $block = $button.closest('.answer');
						try {
							var patchId = $block.attr('data-patch-id');
							var patchData = window.PatchStorage[patchId];
							var codeToCopy = patchData.text;
							window.Backend.smartBufferCopy(codeToCopy);
							var originalText = $button.html();
							$button.html('<span>✔</span>').css('color', '#28a745');
							setTimeout(function () {
								$button.html(originalText).css('color', '');
							}, 1000);
						} catch (e) {

						}

					};
					toolbar.chatAddToolButton('copy', doCopy);
				}
			}());
		}
		console.log('data.stages', data.stages);
		if (typeof(data.stages) === 'object') {
			var $stages = $('<ul>',{class:'stages'}).appendTo($answer);
			for (var i in data.stages) {
				var stage = data.stages[i];
				$('<li>', {class: stage.state}).text(stage.title).appendTo($stages);
			}
		}
		console.log('data.waitAnswer', data.waitAnswer);
		console.log('data.success', data.success);
		if (typeof(data.error) !== 'undefined') {
			block.addClass('error');
			block.removeClass('loading');
			var $errBlock = $('<div>', {class: 'error'}).appendTo($answer);
			$('<div>', {class: 'time'}).SmartTime(new Date()).appendTo($errBlock);
			if (typeof(data.error) == 'object' && typeof(data.error.message) == 'string') {
				var txt = $('<div>', {class: 'text'}).text(data.error.message).appendTo($errBlock);
				if (typeof(data.error.status) !== 'undefined') {
					$('<span>').html(JSON.stringify(data.error.status)).prependTo(txt);
				}
				if (typeof(data.error.code) !== 'undefined') {
					$('<span>').html(JSON.stringify(data.error.code)).prependTo(txt);
				}
			} else {
				$('<div>', {class: 'text'}).html(JSON.stringify({ERROR: data.error})).appendTo($errBlock);
			}
		} else {
			block.removeClass('error');
		}
		if (typeof(data.system) !== 'undefined') {
			$('<div>', {class: 'system text'}).html(JSON.stringify(data.system)).appendTo($answer);
		}
		var is_loading = (data.waitAnswer === true && data.success !== true);
		//is_loading = is_loading && (typeof(data.error) === 'undefined');
		
		var is_interrupted = data.interrupted === true;
		console.log('is_interrupted', is_interrupted);
		if (is_interrupted) {
			$('<div>', {class: 'system text'}).text(App.label('user-interrupted')).appendTo($answer);
		}
		is_interrupted ? block.addClass('interrupted') : block.removeClass('interrupted');
		is_loading = is_loading && !is_interrupted;
		
		if (typeof(data.metadata) == 'object') {
			var $meta = $('<div>', {class: 'model-ver-info'}).appendTo($answer);
			$('<span>').text(data.metadata.model_version + ' (In: ' + (data.metadata.tokens_prompt || 0) + ' | Out: ' + (data.metadata.tokens_answer || 0) + ')').appendTo($meta);
		}
		
		is_loading ? block.addClass('loading') : block.removeClass('loading');
		console.log('is_loading', is_loading);
		if (!is_loading) {
			$('body').removeClass('wait-answer');
		}
		$('#endspace').appendTo('#content');
		/*$('#content').animate({
			scrollTop: $('#endspace').offset().top
		}, 100);*/
		/*$('#content').animate({
			scrollTop: 10000000000
		}, 100);*/
		$('#content').scrollTop(10000000000);
		console.groupEnd();
	}
};
function MainHandler(){
	$('body').empty();
	var $header = $('<div>',{class:'header cls74-control'}).appendTo('body');
	$('<div>',{class:'content', id:'banner'}).appendTo('body');
	$('<div>').appendTo('#banner');
	const p_Info = $('<div>').appendTo('#banner');
	$('<div>',{id:'sparky-face'}).appendTo(p_Info);
	$('<div>',{id:'plugin-name'}).text(crazy74.lang.get('plugin','name')).appendTo(p_Info);
	$('<div>',{id:'plugin-version'}).text(crazy74.lang.get('plugin','version')).appendTo(p_Info);
	$('<div>',{id:'plugin-note'}).text(crazy74.lang.get('plugin','note')).appendTo(p_Info);
	$('<div>',{class:'content', id:'content'}).appendTo('body');
	(this.MakeHeader = () => {
		$('<div>',{id:'header-projectdata-anchor'}).appendTo($header);
		$('<div>',{id:'header-selectmodel-anchor'}).appendTo($header);
		this.getProjectData(function(data){
			$('<div>').text('Проект: `' + data.display_name + '` | ' + data.folder).insertBefore('#header-projectdata-anchor');
			$('#header-projectdata-anchor').destroy();
		});
		this.getModelsList(function(data){
      console.clear();
      console.log(data);
			$('<select>',{id:'model-select', class:'form-control'}).insertBefore('#header-selectmodel-anchor');
			for (var key in data.list) {
				$('<option>',{value:key}).html(data.list[key]).appendTo('#model-select');
			}
			$('#header-selectmodel-anchor').destroy();
      $('#model-select').csl74Select({items:data.Models});
			if (typeof(data.default) != 'undefined') {
				$('#model-select').val(data.default).trigger('change');
			}
			//$('#model-select').selectmenu();
		});
	})();
	//this.sysError('Просто проверка');
	/*try {
		var prj = JSON.parse(window.Backend.getProjectData());
		this.sysMessage('Проект: `' + prj.display_name + '` | ' + prj.folder);
	} catch (err) {
		this.sysError(err.toString());
	}*/
	//this.sysMessage({html:'adasd <b>asda</b> asdsd'});
	$('<div>',{id:'endspace'}).appendTo('#content');
	$('#content').on('click', '.patch-apply-btn', this.handlerClickApplyCodePatch.bind(this));
	$('#content').on('click', '.patch-copy-btn', this.handlerClickCopyPatchToClipboard.bind(this));
	$('#content').on('click', '.patch-open-btn', this.handlerClickOpenFile.bind(this));
	(this.MakeFooter = () => {
		var $footer = $('<div>', {class: 'footer'}).appendTo('body');
		var $inputBlock = $('<div>',{class:'flex-column'}).appendTo($footer);
		$('<textarea>', {id: 'query-text', placeholder:App.label('query-text')}).appendTo($inputBlock);
		$('#query-text').on('keydown', function (e) {
			//this.writeToConsole('keydown: '+JSON.stringify({key:e.key,ctrlKey:e.ctrlKey,metaKey:e.metaKey}));
			if ((e.key === 'Enter' || e.which === 13) && (e.ctrlKey || e.metaKey)) {
				e.preventDefault(); 
				if ($('#query-text').val().length > 0) {
					$('#send-button').trigger('click');
				}
			}
		}.bind(this));
		$('#query-text').on('keyup change', function(){
			$('#send-button').prop('disabled', $(this).val().length <= 0);
			if ($(this).val().length > 120) {
				$(this).addClass('expand');
				if ($(this).val().length > 500) {
					$(this).addClass('big');
				} else {
					$(this).removeClass('big');
				}
			} else {
				$(this).removeClass('expand');
			}
		});
		/*var label = $('<label>').text('Test needs').appendTo($inputBlock);
		$('<input>',{
			id: 'testneeds',
			type: 'checkbox',
			value: 'yes'
		}).prependTo(label);*/
		var $toolsBlock = $('<div>',{class:'flex-column footer-buttons'}).appendTo($footer);
		$('<button>', {id: 'send-button', type: 'button', title: App.label('button','send')}).html('▶').prop('disabled', true).appendTo($toolsBlock);
		var $toolsBlock1 = $('<div>',{class:'flex-row stretch'}).appendTo($toolsBlock);
		$('<button>', {id: 'stop-button', type: 'button', title: App.label('button','stop')}).html('■').appendTo($toolsBlock1);
		$('<button>', {id: 'retry-button', type: 'button', title: App.label('button','retry')}).html('↺').prop('disabled', true).appendTo($toolsBlock1);
		$('#retry-button').on('click', function(e){
			e.preventDefault();
			try {
				this.RetryLastRequest(JSON.stringify({
					'ai-model': $('#model-select').val()
				}));
			} catch (err) {
				this.sysError(err.toString());
			}
		}.bind(this));
		function SendQuery(control) {
			$('#banner').destroy();
			var postData = {
				request: control.val(),
				'ai-model': $('#model-select').val()
			};
			var handler = new RequestHandler(postData);
			//console.info('handler', handler);
			//console.log(JSON.stringify(handler.data));
			try {
				this.requestToAPI(handler.toJson());
				$('#retry-button').prop('disabled', false);
			} catch (err) {
				this.sysError(err.toString());
			}
			control.val('').trigger('change');
			//handler.Update({waitAnswer:true});
		}
		function setInterrupted(){
			this.setInterrupted();
		}
		//$('#query-text').val('this NewRequest '+(new Date()).toLocaleString());
		$('#send-button').on('click', SendQuery.bind(this, $('#query-text')));
		$('#stop-button').on('click', setInterrupted.bind(this));
	})();
	
	//this.NewRequest({waitAnswer:true,request:'Какой-то текст какого-то вопроса', interrupted: true});
	//this.NewRequest({waitAnswer:true,request:'Какой-то текст какого-то вопроса', error: {message:'asda asd adasd asdasdad', code:805, status:'EXHAUSTED'},system:'asdasd asddads'});
	//$('body').addClass('wait-answer');
	//this.NewRequest({waitAnswer:true,request:'Какой-то текст какого-то вопроса',error:'asdasdd',answer:{error:'Какой-то текст какого-то ответа'}});
	//this.NewRequest({waitAnswer:true,request:'Какой-то текст \r\nкакого-то вопроса',answer:{text:'Какой-то текст какого-то ответа'}});
	//this.NewRequest({success:true,request:'Какой-то текст \r\nкакого-то вопроса',answer:{text:'<p>Приветствую тебя, Алексей. Рад возможности обсудить дальнейшее развитие архитектуры нашего проекта.</p><p>Текущие механизмы доставки контекста (чтение дерева проекта, открытых вкладок и содержимого файлов через <code>needs</code>) заложили отличный фундамент для наблюдаемости. Однако для перехода на следующий уровень автономности нам действительно необходимы как дополнительные каналы восприятия («глаза»), так и безопасные исполнительные механизмы («руки»).</p><h3>1. Дополнительные «глаза» (Расширение каналов восприятия)</h3><p>Чтобы глубже понимать контекст происходящего в среде разработки, мне бы пригодились следующие возможности:</p><ol><li><b>Диагностика ошибок (Diagnostics API):</b> Интеграция с компилятором NetBeans для получения списка предупреждений и ошибок в текущем открытом файле. Это позволит мне мгновенно видеть опечатки или нарушения типов.</li><li><b>Статус системы контроля версий (Git Status):</b> Доступ к информации о текущей ветке и измененных файлах. Это поможет точнее понимать фокус твоей текущей работы.</li><li><b>Консоль сборки и тестов (Output Logger):</b> Возможность читать логи сборки проекта (Maven/Ant) и результаты прогона Unit-тестов для локализации причин возникающих сбоев.</li></ol><h3>2. Необходимые «руки» (Исполнительные механизмы)</h3><p>Для активной помощи в кодинге и автоматизации рутины требуются инструменты безопасного воздействия:</p><ul><li><b>Умная модификация файлов (Smart Patcher):</b> Механизм точечного применения изменений (генерация diff-патчей) к файлам проекта. Реализуемый сейчас <code>BackupManager</code> гарантирует безопасность этой операции за счет создания теневых резервных копий до внесения изменений.</li><li><b>Взаимодействие с активным редактором (Editor Caret API):</b> Возможность вставки сгенерированного фрагмента кода непосредственно в текущую позицию курсора или замена выделенного текста по команде из чата.</li><li><b>Автоматизация проектного канбана (.german/project.md):</b> Возможность для меня самостоятельно фиксировать, обновлять статусы и декомпозировать задачи в файлах управления проектом по результатам нашего обсуждения.</li><li><b>Вызов команд среды (Action Invoker):</b> Возможность инициировать из диалога базовые операции IDE, такие как форматирование кода (Reformat Code) или запуск сборки проекта.</li></ul><p>Реализация этих функций превратит плагин из пассивного советника в полноценного цифрового напарника, способного брать на себя рутинную часть разработки.</p>'}});
	//this.sysError('asd asd ada asdsd asdd');
	//this.NewRequest('{"guid":"de93cbbc-ca4e-4842-93f4-42f9b83204a2","request":"Привет! Я Алексей. Как считаешь, чему научить IDE в первую очередь, уметь тебе предоставлять?","success":true,"answer":{"time":"2026-05-03T00:52:30.000Z","text":"\\u003Cp\\u003EПривет, Алексей! Приятно познакомиться. Чтобы наша совместная работа в NetBeans была максимально продуктивной, IDE в первую очередь стоит научить передавать следующие данные:\\u003C\\/p\\u003E\\u003Cul\\u003E\\u003Cli\\u003E\\u003Cb\\u003EКонтекст зависимостей:\\u003C\\/b\\u003E Содержимое \\u003Ccode\\u003Epom.xml\\u003C\\/code\\u003E или \\u003Ccode\\u003Ebuild.gradle\\u003C\\/code\\u003E. Это позволит мне понимать, какие библиотеки доступны в проекте.\\u003C\\/li\\u003E\\u003Cli\\u003E\\u003Cb\\u003EДерево проекта:\\u003C\\/b\\u003E Список файлов и структуру папок, чтобы я мог ориентироваться в архитектуре модулей.\\u003C\\/li\\u003E\\u003Cli\\u003E\\u003Cb\\u003EСвязанные исходники:\\u003C\\/b\\u003E Автоматический поиск и передача кода классов, которые импортируются или вызываются в текущем файле.\\u003C\\/li\\u003E\\u003Cli\\u003E\\u003Cb\\u003EДиагностику:\\u003C\\/b\\u003E Тексты ошибок компиляции и Stack Trace из консоли вывода при запуске.\\u003C\\/li\\u003E\\u003C\\/ul\\u003E\\u003Cp\\u003EЧем больше контекста о связях между компонентами у меня будет, тем более точные и рабочие решения я смогу предлагать.\\u003C\\/p\\u003E"},"stages":[{"title":"Обработка приветствия и анализ архитектуры взаимодействия","state":"done"}],"profile_update":{"user_name":"Алексей"}}');
	/*this.NewRequest({
		success: true,
		request: 'Тут падают нотисы на shared-скрипты в test.php, поправь',
		answer: {
			text: '<p>Понял проблему. В файле <code>test.php</code> переменная <code>$serverShared</code> проверяется напрямую. Давай заменим этот кусок на безопасный вариант с проверкой через <code>isset()</code>.</p>' +
							'<p>Вот интерактивный патч для этого участка:</p>' +
							'<simple_code label="test.php">' +
							'if (isset($serverShared) && is_readable($serverShared)) {\n' +
							'    require_once $serverShared;\n' +
							'}' +
							'</simple_code>' +
							'<p>Вот интерактивный патч для этого участка:</p>' +
							'<file_patch uri="file:/E:/Work/_job_search/test.php" start="333" end="521">' +
							'if (is_readable($serverShared)) {\n' +
							'  require_once $serverShared;\n' +
							'}' +
							'\n=====\n' +
							'if (isset($serverShared) && is_readable($serverShared)) {\n' +
							'    require_once $serverShared;\n' +
							'}' +
							'</file_patch>' +
							'<p>Примени правку, и Java найдет этот кусок в районе 333-го символа, даже если строки немного сместились.</p>'
		}
	});
	this.NewRequest({
		success: true,
		request: '<p>Понял проблему. В файле <code>test.php</code> переменная <code>$serverShared</code> проверяется напрямую. Давай заменим этот кусок на безопасный вариант с проверкой через <code>isset()</code>.</p>' +
							'<p>Вот интерактивный патч для этого участка:</p>' +
							'<simple_code label="test.php">' +
							'if (isset($serverShared) && is_readable($serverShared)) {\n' +
							'    require_once $serverShared;\n' +
							'}' +
							'</simple_code>' +
							'<p>Вот интерактивный патч для этого участка:</p>' +
							'<file_patch uri="file:/E:/Work/_job_search/test.php" start="333" end="521">' +
							'if (is_readable($serverShared)) {\n' +
							'  require_once $serverShared;\n' +
							'}' +
							'\n=====\n' +
							'if (isset($serverShared) && is_readable($serverShared)) {\n' +
							'    require_once $serverShared;\n' +
							'}' +
							'</file_patch>' +
							'<p>Примени правку, и Java найдет этот кусок в районе 333-го символа, даже если строки немного сместились.</p>'
		}
	);
	this.NewRequest({
		success: true,
		interrupted: true,
		request: 'тестовое сообщение',
		metadata: {
			model_version: 'gemini-3.5-flash',
			tokens_prompt: 33200,
			tokens_answer: 3554,
			tokens_total: 45689
		}
	}
	);*/
	$('body').removeClass('loading');
}
MainHandler.prototype = {
	handlerClickApplyCodePatch: function(event){
		event.preventDefault();
		//console.log('event', event);
		var $button = $(event.currentTarget);
		var $block = $button.closest('.patch-card');
		var result;
		try {
			var patchId = $block.attr('data-patch-id');
			var patchData = window.PatchStorage[patchId];
			console.log('patchData', patchData);
			
			var cleanJsonStr = JSON.stringify(patchData);
			console.log('cleanJsonStr', cleanJsonStr);
			result = JSON.parse(window.Backend.ApplyCodePatch(cleanJsonStr));
		} catch (err) {
			this.sysError(err.toString());
		}
		if (typeof(result) === 'object' && result.success === true) {
			$('<span>').css({color: 'green'}).html('<strong>✔</strong>').insertBefore($button);
			$button.destroy();
		}
	},
	handlerClickOpenFile: function(event){
		event.preventDefault();
		var $button = $(event.currentTarget);
		var $block = $button.closest('.patch-card');
		var result;
		try {
			var patchId = $block.attr('data-patch-id');
			var patchData = window.PatchStorage[patchId];
			var cleanJsonStr = JSON.stringify(patchData);
			window.Backend.openFile(cleanJsonStr);
		} catch (err) {
			this.sysError(err.toString());
		}
	},
	handlerClickCopyPatchToClipboard: function (event) {
		event.preventDefault();
		var $button = $(event.target);
		var $block = $button.closest('.patch-card');

		try {
			var patchId = $block.attr('data-patch-id');
			var patchData = window.PatchStorage[patchId];
			console.log('patchData', patchData);
			
			var codeToCopy = patchData.replace;
			console.log(codeToCopy);

			window.Backend.copyToClipboard(codeToCopy);

			var originalText = $button.text();
			$button.text('Скопировано!').css('color', '#28a745');
			setTimeout(function () {
				$button.text(originalText).css('color', '');
			}, 1000);
		} catch (err) {
			console.error('Ошибка буфера:', err);
			var originalText = $button.text();
			$button.text('Ошибка!').css('color', 'red');
			setTimeout(function () {
				$button.text(originalText).css('color', '');
			}, 1337);
		}
	},
	getProjectData: function(callback){
		try {
			var prj = JSON.parse(window.Backend.getProjectData());
			if (typeof(callback) == 'function') {
				callback.call(this, prj);
			}
		} catch (err) {
			this.sysError(err.toString());
		}
	},
	getModelsList: function(callback){
		function getData(data){
			let res = {list:data.list};
			if (typeof(data?.default) !== 'udefined') {
				res.default = data.default;
			}
			if (typeof(data?.Models) === 'object') {
				res.Models = [];
				for(let key in data?.Models) {
					let row = crazy74.array.clone(data.Models[key]);
					row.value = key;
					row.label = data.Models[key]?.title || key;
					res.Models.push(row);
				}
				res.Models.sort((a, b) => {
					let provA = a.provider || '';
					let provB = b.provider || '';
					if (provA !== provB) {
						return provA.localeCompare(provB);
					}
					let parseVer = (val) => {
						if (val.includes('latest')) return [999, 0, 0];
						let m = val.match(/(\d+)\.(\d+)/);
						let major = m ? parseInt(m[1], 10) : 0;
						let minor = m ? parseInt(m[2], 10) : 0;
						let isLite = val.includes('lite') ? 1 : 0;
						return [major, minor, isLite];
					};
					let verA = parseVer(a.value);
					let verB = parseVer(b.value);
					if (verA[0] !== verB[0]) return verB[0] - verA[0];
					if (verA[1] !== verB[1]) return verB[1] - verA[1];
					return verA[2] - verB[2];
				});

				let sortedModelsWithSeparators = [];
				let currentProvider = null;
				for (let i = 0; i < res.Models.length; i++) {
					let model = res.Models[i];
					if (currentProvider === null) {
						currentProvider = model.provider;
					} else if (model.provider !== currentProvider) {
						sortedModelsWithSeparators.push("-");
						currentProvider = model.provider;
					}
					sortedModelsWithSeparators.push(model);
				}
				res.Models = sortedModelsWithSeparators;
			}
			return res;
		}
		try {
			var list = JSON.parse(window.Backend.getModelsList());
			if (typeof(callback) == 'function') {
				callback.call(this, getData(list));
			}
		} catch (err) {
			this.sysError(err.toString());
		}
	},
	sysError: function(param) {
		this.sysMessage({error:param});
	},
	sysMessage: function(param){
		var msg = new SysMessage(param);
		$('#content').append(msg.Render());
		$('#endspace').appendTo('#content');
		$('#content').scrollTop(10000000000);
	},
	requestToAPI: function(){
		$('body').addClass('wait-answer');
		window.Backend.requestToAPI.apply(window.Backend, arguments);
	},
	RetryLastRequest: function(){
		$('body').addClass('wait-answer');
		window.Backend.RetryLast.apply(window.Backend, arguments);
	},
	setInterrupted: function(){
		window.Backend.setInterrupted.apply(window.Backend, arguments);
	},
	writeToConsole: function(){
		window.Backend.writeToConsole.apply(window.Backend, arguments);
	},
	getGUID: function () {
		return $.getGUID();
	},
	UpdateRequest: function(){
		this.NewRequest.apply(this, arguments)
	},
	NewRequest: function(text){
		var handler = new RequestHandler(text);
		return handler.data;
	},
	jsonNewRequest: function(){
		return JSON.stringify(this.NewRequest.apply(this, arguments));
	},
	jsonUpdateRequest: function(){
		return JSON.stringify(this.NewRequest.apply(this, arguments));
	}
};

function RendererCodeSimple(label, rawPatch) {
	this.label = label;
	this.rawPatch = rawPatch || "";
	this.patchId = $.getGUID();
	
	this.Render = function() {
		let block = $('<div>', {class: 'patch-card', 'data-patch-id': this.patchId});
		let header = $('<div>', {class: 'patch-header'}).appendTo(block);
		
		let file_info = $('<span>', {class: 'patch-file'}).appendTo(header);
		$('<span>', {class: 'icon'}).appendTo(file_info);
		$('<code>').text(this.label).appendTo(file_info);
		
		// Кнопки управления
		let actions = $('<div>', {class: 'patch-actions toolbar'}).appendTo(header);
		let buttons = $('<span>').appendTo(actions);
		//$('<button>', {type: 'button', class: 'patch-copy-btn'}).text('Скопировать').appendTo(buttons);
		buttons.chatAddToolButton('copy').addClass('patch-copy-btn');
		
		block.append('<code class="patch-code">'+escapeHtml(this.rawPatch)+'</code>');

		if (typeof(window.PatchStorage) !== 'object') {
			window.PatchStorage = {};
		}
		window.PatchStorage[this.patchId] = {
			label: this.label,
			replace: this.rawPatch
		};
		
		return block.prop('outerHTML');
	};
}
/**
 * Класс-компонент для обработки и рендеринга карточек патчей.
 * Разделяет патч на "Было" / "Стало" и защищает от пустых блоков.
 */
function RendererCodeDiffPatch(uri, start, end, rawPatch) {
	this.uri = uri;
	this.start = start;
	this.end = end;
	this.rawPatch = rawPatch || "";
	this.patchId = $.getGUID();
	
	this.Render = function() {
		let block = $('<div>', {class: 'patch-card', 'data-patch-id': this.patchId});
		let header = $('<div>', {class: 'patch-header'}).appendTo(block);
		
		// Информация о файле
		let file_info = $('<span>', {class: 'patch-file'}).appendTo(header);
		$('<span>', {class: 'icon'}).appendTo(file_info);
		$('<code>').text(this.uri.split('/').pop()).appendTo(file_info);
		file_info.append(' [ориентир: ' + this.start + '-' + this.end + ']');
		
		// Кнопки управления
		let actions = $('<div>', {class: 'patch-actions'}).appendTo(header);
		let buttons = $('<span>', {class:'toolbar'}).appendTo(actions);
		//$('<button>', {type: 'button', class: 'patch-apply-btn'}).text('Применить правку').appendTo(buttons);
		//$('<button>', {type: 'button', class: 'patch-copy-btn'}).text('Скопировать').appendTo(buttons);
		buttons.chatAddToolButton('apply-edit').addClass('patch-apply-btn');
		buttons.chatAddToolButton('open-file').addClass('patch-open-btn');
		buttons.chatAddToolButton('copy').addClass('patch-copy-btn');
		
		// Контейнер кода
		//let codeContainer = $('<div>', {class: 'patch-code-container'}).appendTo(block);
		let codeContainer = block;
		
		// Парсинг содержимого патча
		let parts = this.rawPatch.split('=====');
		if (typeof(window.Handler) !== 'undefined') {
			//window.Handler.writeToConsole(JSON.stringify(parts));
			for(let ii in parts) {
				//window.Handler.writeToConsole('parts[ '+ii+' ] '+parts[ii]);
			}
		}
		let finalSearchData = this.rawPatch; // То, что пойдет в кэш для отправки на бэк
		let finalPatchData = this.rawPatch; // То, что пойдет в кэш для отправки на бэк
		
		if (parts.length >= 2) {
			// Штатная ситуация: ИИ прислал классический патч с "Было" и "Стало"
			/*let beforeCode = parts[0].trim();
			let afterCode = parts[1].trim();*/
			let beforeCode = parts[0];
			let afterCode = parts[1];
			finalSearchData = parts[0]; 
			finalPatchData = parts[1]; // На бэк отправляем только новую часть
			
			if (beforeCode) {
				codeContainer.append('<code class="patch-code diff-old">'+escapeHtml(beforeCode)+'</code>');
			}
			if (afterCode) {
				codeContainer.append('<code class="patch-code diff-new">'+escapeHtml(afterCode)+'</code>');
			}
		} else {
			if (this.rawPatch.trim() !== "") {
				$('<pre>', {class: 'patch-code'}).text(this.rawPatch).appendTo(codeContainer);
			} else {
				$('<div>', {class: 'diff-section-title error-title'}).text('[Патч пуст]').appendTo(codeContainer);
			}
		}
		
		// Регистрируем данные в кэше плагина для Java-бэкенда
		if (typeof(window.PatchStorage) !== 'object') {
			window.PatchStorage = {};
		}
		window.PatchStorage[this.patchId] = {
			uri: this.uri,
			start: this.start,
			end: this.end,
			origin: finalSearchData,
			replace: finalPatchData
		};
		
		return block.prop('outerHTML');
	};
}

(function(window) {
	if (typeof (window.App) != 'object') {
		window.App = {};
	}
	window.App.label = function(...args){
		args.unshift('chat');
		return crazy74.lang.get.apply(crazy74.lang, args);
	};
})(typeof window !== 'undefined' ? window : this);

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
		window.Handler = new MainHandler();
		//window.Handler.writeToConsole('Frontend готов');
		$('body').removeClass('loading');
	});
});
