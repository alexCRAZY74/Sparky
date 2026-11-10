
function numWithEnding(number, endings, nullStr) {
	return crazy74.number.withEnding.apply(crazy74.number, arguments);
}

function parseBool(string) {
	return crazy74.bool.apply(this, arguments);
}

function escapeHtml(text) {
	return crazy74.string.escapeHtml.apply(this, arguments);
}

function notEmpty(val) {
	return crazy74.array.notEmpty(val);
}

function clone(obj) {
	return crazy74.array.clone(obj);
}

function assignData(data, excludes) {
	return crazy74.array.assignData(this, data, excludes);
}

(function($) {
    // Дефолтные настройки
		$.cslWindow = $.fn.cslWindow = $.fn.csl74Window;
		return;
    const defaults = {
				icon: false,
        title: 'Окно',
        content: '',
        customClass: '',
        contentStyle: '',
        withBackDrop: true,
        destroyOnClose: true,
        closeOnEsc: true,
        duration: 150,
        width: null,        // '500px' или 500
        height: null,       // '400px' или 400
        labels: {
            close: 'Закрыть',
            title: 'Информационное окно'
        },
				'z-group': 'body > *, .csl-window-frame, .csl-window-backdrop, .-webkit-scrollbar',
        onClose: null
    };

    $.cslWindow = $.fn.cslWindow = function(options) {
        const opt = $.extend(true, {}, defaults, options);
				const optZIndex = { inc: 100, group: opt['z-group'] };

        // Формирование BEM-подобных или префиксных классов
        const getClassName = (postfix) => {
            let cls = 'csl-window';
            if (postfix) cls += '-' + postfix;
            if (opt.customClass) {
                cls += ' ' + opt.customClass;
                if (postfix) cls += '-' + postfix;
            }
            return cls;
        };

        let $backdrop = null;
        let $frame = null;
        let $content = null;

        // Если вызов идет от элементов $(selector).cslWindow()
        const isElementCall = this && this.jquery && this.length > 0;

        // Элемент бакдропа
        if (opt.withBackDrop) {
            $backdrop = $('<div>', { class: getClassName('backdrop') }).appendTo('body');
            if (typeof $.fn.maxZIndex === 'function') {
                $backdrop.maxZIndex(optZIndex);
            }
        }

        // Основной фрейм
        $frame = $('<div>', { class: getClassName('frame') }).appendTo('body');
        if (typeof $.fn.maxZIndex === 'function') {
            $frame.maxZIndex(optZIndex);
        }

        // Шапка окна
        const $header = $('<div>', { class: getClassName('header') }).appendTo($frame);
        const $closeBtn = $('<button>', { 
						type: 'button',
            class: getClassName('close'), 
            title: opt.labels.close,
            html: '&times;'
        }).appendTo($header);
        
        let $title = $('<div>', { 
            class: getClassName('title'), 
            text: opt.title || opt.labels.title 
        }).appendTo($header);
				if (typeof(opt.icon) == 'string') {
					$title.wrapInner('<span>');
					$('<span>', {class: 'icon '+opt.icon}).prependTo($title);
				}

        // Контейнер под содержимое
        $content = $('<div>', { class: getClassName('content') }).appendTo($frame);
        if (opt.contentStyle) {
            $content.addClass(opt.contentStyle);
        }

        // Заполнение контентом
        if (isElementCall) {
            $content.append(this.show());
        } else if (opt.content) {
            $content.append(opt.content);
        }

        // Применение размеров
        if (opt.width) {
            $frame.css('width', typeof opt.width === 'number' ? `${opt.width}px` : opt.width);
        }
        if (opt.height) {
            $content.css('height', typeof opt.height === 'number' ? `${opt.height}px` : opt.height);
        }

        // Функция закрытия
        const close = () => {
            $(window).off('keydown.cslWindow');
            
            if ($backdrop) {
                $backdrop.fadeOut(opt.duration, () => {
                    if (opt.destroyOnClose) $backdrop.remove();
                });
            }

            $frame.fadeOut(opt.duration, () => {
                if (opt.destroyOnClose) $frame.remove();
                if (typeof opt.onClose === 'function') {
                    opt.onClose();
                }
            });
        };

        // Навешивание клика закрытия
        $closeBtn.on('click', close);

        // Закрытие по ESC
        if (opt.closeOnEsc) {
            $(window).on('keydown.cslWindow', (e) => {
                if (e.key === 'Escape' || e.keyCode === 27) {
                    close();
                }
            });
        }

        // Показываем окно
        if ($backdrop) $backdrop.fadeIn(opt.duration);
				if (opt.position && typeof $.fn.position === 'function') {
					$frame.show().position(opt.position);
					setTimeout(() => {
						$frame.position(opt.position);
					}, 50);
				} else {
					$frame.addClass('csl-window-centered').fadeIn(opt.duration);
				}

        // Публичный интерфейс управления окном
        return {
            $frame: $frame,
            $content: $content,
            close: close,
            setTitle: function(newTitle) {
                $header.children('.' + getClassName('title')).text(newTitle);
            },
            setContent: function(newContent) {
                $content.empty().append(newContent);
            }
        };
    };

    $.fn.cslWindow.defaults = defaults;

})(jQuery);