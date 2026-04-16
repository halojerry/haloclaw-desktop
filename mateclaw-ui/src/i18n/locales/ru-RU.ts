// Ozon-Claw 俄文国际化文件 (ru-RU)
export default {
  app: {
    title: 'Ozon-Claw - AI Ассистент',
  },
  common: {
    save: 'Сохранить',
    cancel: 'Отмена',
    reset: 'Сбросить',
    edit: 'Редактировать',
    delete: 'Удалить',
    create: 'Создать',
    update: 'Обновить',
    loading: 'Загрузка...',
    enabled: 'Включено',
    disabled: 'Отключено',
    default: 'По умолчанию',
    view: 'Просмотр',
    copy: 'Копировать',
    copied: 'Скопировано',
    confirm: 'Подтвердить',
    add: 'Добавить',
    search: 'Поиск',
    expandSidebar: 'Развернуть боковую панель',
    collapseSidebar: 'Свернуть боковую панель',
    manageWorkspaces: 'Управление рабочими пространствами',
    show: 'Показать',
    hide: 'Скрыть',
    on: 'Вкл',
    off: 'Выкл',
    yes: 'Да',
    no: 'Нет',
    configure: 'Настроить',
    enable: 'Включить',
    disable: 'Отключить',
    close: 'Закрыть',
  },
  chat: {
    status: {
      idle: 'Готов',
      streaming: 'Генерация...',
      error: 'Отключено',
    },
    thinking: 'Мысли',
    thinkingInProgress: 'Думаю...',
    stopped: 'Генерация остановлена',
    interrupted: 'Прервано',
    failed: 'Ошибка генерации',
    compressionSummary: 'Предыдущие разговоры были обобщены',
    compressionWithCount: '{count} предыдущих сообщений обобщено',
    loadingOlder: 'Загрузка более ранних сообщений...',
    loadOlderMessages: 'Нажмите для загрузки более ранних сообщений',
    retry: 'Повторить',
    errorCode: 'Код ошибки',
    error: {
      rate_limit: {
        title: 'Слишком много запросов',
        description: 'Вы превысили лимит запросов. Сервер временно не может обработать ваш запрос.',
        action: 'Пожалуйста, подождите и повторите попытку или сократите количество последовательных запросов.',
      },
      auth_expired: {
        title: 'Сессия истекла',
        description: 'Ваша сессия истекла, вам нужно войти снова.',
        action: 'Вы будете перенаправлены на страницу входа.',
      },
      forbidden: {
        title: 'Доступ запрещен',
        description: 'У вас нет разрешения на выполнение этого действия.',
        action: 'Пожалуйста, свяжитесь с администратором для получения доступа.',
      },
      bad_request: {
        title: 'Неверный запрос',
        description: 'Запрос был неправильно сформирован и не может быть обработан сервером.',
        action: 'Пожалуйста, проверьте ввод и повторите попытку.',
      },
      server_error: {
        title: 'Ошибка сервера',
        description: 'При обработке вашего запроса произошла ошибка сервера.',
        action: 'Пожалуйста, попробуйте позже. Если проблема не исчезнет, свяжитесь с администратором.',
      },
      service_unavailable: {
        title: 'Сервис недоступен',
        description: 'Сервис находится на обслуживании или временно перегружен.',
        action: 'Пожалуйста, попробуйте позже.',
      },
      timeout: {
        title: 'Время ожидания истекло',
        description: 'Сервер не ответил вовремя. Это может быть связано с проблемами сети или высокой нагрузкой.',
        action: 'Пожалуйста, проверьте подключение к интернету и повторите попытку.',
      },
      network: {
        title: 'Ошибка сети',
        description: 'Не удалось подключиться к серверу. Пожалуйста, проверьте подключение к интернету.',
        action: 'Убедитесь, что сеть работает, и повторите попытку.',
      },
      unknown: {
        title: 'Неожиданная ошибка',
        description: 'При обработке запроса возникла непредвиденная проблема.',
        action: 'Пожалуйста, попробуйте повторить. Если проблема не исчезнет, свяжитесь с администратором.',
      },
    },
    copy: 'Копировать',
    copied: 'Скопировано',
    regenerate: 'Перегенерировать',
    ttsPlay: 'Озвучить',
    ttsStop: 'Остановить',
    conversations: 'Список разговоров',
    newChat: 'Новый чат',
    loadingAgents: 'Загрузка агентов...',
    selectAgent: 'Выберите агента',
    noConversations: 'Нет разговоров',
    startNewChat: 'Начните новый разговор',
    messages: '{count} сообщений',
    configModel: 'Настроить модель',
    clearMessages: 'Очистить сообщения',
    goToModelSettings: 'Перейти к настройкам модели',
    configModelFirst: 'Сначала настройте модель',
    modelUnavailable: 'Текущая модель недоступна',
    noActiveModel: 'Нет активной модели. Сначала выберите доступную модель в настройках.',
    noAvailableModel: 'Нет доступных моделей.',
    loadAgentsFailed: 'Не удалось загрузить список агентов',
    loadModelFailed: 'Не удалось загрузить статус модели',
    loadConversationsFailed: 'Не удалось загрузить список разговоров',
    loadMessagesFailed: 'Не удалось загрузить историю сообщений',
    deleteConversationFailed: 'Не удалось удалить разговор',
    deleteConfirm: 'Удалить этот разговор? Это действие нельзя отменить.',
    switchModelFailed: 'Не удалось переключить модель',
    searchModel: 'Поиск модели...',
    noMatchModel: 'Нет подходящих моделей',
    uploadFailed: 'Ошибка загрузки файла',
    dropToUpload: 'Перетащите файл или папку сюда',
    copyFailed: 'Не удалось скопировать',
    dateToday: 'Сегодня',
    dateYesterday: 'Вчера',
    dateLast7Days: 'Последние 7 дней',
    dateEarlier: 'Ранее',
    messagePlaceholder: 'Введите сообщение... (Enter отправить, Shift+Enter новую строку)',
    subtitle: 'Умный AI-ассистент на базе Ozon-Claw',
  },

  // ==================== Страница входа ====================
  login: {
    title: 'Вход',
    subtitle: 'Добро пожаловать в платформу автоматизации Ozon-Claw',
    signIn: 'Войти',
    signingIn: 'Вход...',
    signUp: 'Регистрация',
    placeholders: {
      username: 'Введите имя пользователя',
      password: 'Введите пароль'
    },
    rememberMe: 'Запомнить меня',
    forgotPassword: 'Забыли пароль?',
    noAccount: 'Нет аккаунта?',
    registerNow: 'Зарегистрироваться',
    deviceInfo: 'Информация об устройстве',
    noDeviceId: 'Устройство не привязано',
    manageDevices: 'Управление устройствами',
    loginSuccess: 'Вход выполнен успешно',
    forgotPasswordHint: 'Свяжитесь с администратором для сброса пароля',
    errors: {
      usernameRequired: 'Введите имя пользователя',
      passwordRequired: 'Введите пароль',
      passwordMinLength: 'Пароль должен содержать не менее 6 символов',
      loginFailed: 'Ошибка входа. Проверьте имя пользователя и пароль'
    }
  },

  // ==================== Страница регистрации ====================
  register: {
    title: 'Регистрация',
    subtitle: 'Создайте аккаунт Ozon-Claw',
    signUp: 'Зарегистрироваться',
    registering: 'Регистрация...',
    placeholders: {
      username: 'Имя пользователя (3-20 символов)',
      email: 'Введите email',
      password: 'Пароль (минимум 6 символов)',
      confirmPassword: 'Подтвердите пароль',
      inviteCode: 'Код приглашения (необязательно)'
    },
    inviteCodeTip: 'Введите код приглашения для получения бонуса',
    agreeTermsPrefix: 'Я прочитал(а) и согласен(на) с',
    termsOfService: 'Условиями использования',
    privacyPolicy: 'Политикой конфиденциальности',
    agreeTermsAnd: 'и',
    hasAccount: 'Уже есть аккаунт?',
    loginNow: 'Войти',
    registerSuccess: 'Регистрация успешна. Перенаправление...',
    passwordStrength: {
      weak: 'Слабый',
      fair: 'Средний',
      good: 'Хороший',
      strong: 'Надежный'
    },
    errors: {
      usernameRequired: 'Введите имя пользователя',
      usernameLength: 'Имя пользователя должно содержать 3-20 символов',
      usernamePattern: 'Имя пользователя может содержать только буквы, цифры и подчёркивание',
      emailRequired: 'Введите email',
      emailInvalid: 'Введите корректный email',
      passwordRequired: 'Введите пароль',
      passwordMinLength: 'Пароль должен содержать не менее 6 символов',
      confirmPasswordRequired: 'Подтвердите пароль',
      passwordMismatch: 'Пароли не совпадают',
      agreeTermsRequired: 'Пожалуйста, примите условия использования и политику конфиденциальности'
    },
    termsContent: 'Условия использования...',
    privacyContent: 'Политика конфиденциальности...'
  },

  // ==================== Управление магазинами ====================
  store: {
    title: 'Управление магазинами',
    description: 'Управление вашими магазинами Ozon, привязка API и синхронизация данных',
    addStore: 'Добавить магазин',
    addFirstStore: 'Добавьте свой первый магазин',
    empty: 'Нет магазинов, нажмите для добавления',
    status: {
      all: 'Все',
      active: 'Активен',
      expired: 'Истёк',
      unauthorized: 'Не авторизован'
    },
    stats: {
      products: 'Товаров',
      orders: 'Заказов',
      revenue: 'Продаж'
    },
    actions: {
      edit: 'Редактировать',
      sync: 'Синхронизировать',
      settings: 'Настройки',
      delete: 'Удалить',
      profitConfig: 'Настройка прибыли'
    },
    bindedAt: 'Привязан',
    syncing: 'Синхронизация...',
    syncSuccess: 'Синхронизация успешна',
    addSuccess: 'Магазин добавлен',
    deleteSuccess: 'Магазин удалён',
    deleteConfirm: {
      title: 'Подтверждение удаления',
      message: 'Вы уверены, что хотите удалить магазин "{name}"? Это действие нельзя отменить.'
    },
    // Настройка прибыли
    profitConfig: {
      title: 'Настройка прибыли',
      reset: 'Сбросить',
      saveSuccess: 'Настройки прибыли сохранены',
      quickView: 'Рентабельность',
      targetRate: 'Целевая рентабельность',
      sections: {
        basic: 'Основные настройки',
        cost: 'Настройки стоимости'
      },
      fields: {
        minProfitRate: 'Минимальная рентабельность',
        targetProfitRate: 'Целевая рентабельность',
        exchangeRate: 'Обменный курс (CNY/RUB)',
        logisticsRate: 'Ставка логистики',
        platformCommission: 'Комиссия платформы'
      },
      hints: {
        minProfitRate: 'Минимальная рентабельность товара, оповещение при снижении',
        targetProfitRate: 'Целевая рентабельность товара, используется для расчёта цены',
        exchangeRate: 'Курс юаня к рублю',
        logisticsRate: 'Логистические расходы в процентах от цены продажи',
        platformCommission: 'Комиссия платформы Ozon в процентах'
      },
      preview: {
        title: 'Предпросмотр цены',
        purchasePrice: 'Закупочная цена',
        sellingPrice: 'Формула цены продажи',
        estimatedPrice: 'Расчётная цена (100 CNY)'
      },
      errors: {
        minProfitRateRequired: 'Введите минимальную рентабельность',
        targetProfitRateRequired: 'Введите целевую рентабельность',
        targetMustGreaterThanMin: 'Целевая рентабельность должна быть больше минимальной',
        exchangeRateRequired: 'Введите обменный курс',
        logisticsRateRequired: 'Введите ставку логистики',
        platformCommissionRequired: 'Введите комиссию платформы'
      }
    },
    form: {
      storeName: 'Название магазина',
      storeNamePlaceholder: 'Введите название магазина',
      storeId: 'ID магазина',
      storeIdPlaceholder: 'Введите ID магазина Ozon',
      apiKey: 'API ключ',
      apiKeyPlaceholder: 'Введите API ключ',
      clientId: 'Client ID',
      clientIdPlaceholder: 'Введите Client ID',
      bindOzonAccount: 'Привяжите ваш аккаунт продавца Ozon',
      bindHint: 'Для получения API-учётных данных выполните следующие шаги:',
      step1: 'Войдите в личный кабинет Ozon Seller',
      step2: 'Перейдите в "Настройки" → "API ключи"',
      step3: 'Скопируйте Client ID и API ключ в форму выше',
      openOzonSeller: 'Открыть Ozon Seller'
    },
    errors: {
      loadFailed: 'Не удалось загрузить список магазинов',
      storeNameRequired: 'Введите название магазина',
      storeIdRequired: 'Введите ID магазина',
      apiKeyRequired: 'Введите API ключ',
      clientIdRequired: 'Введите Client ID'
    }
  },

  // ==================== Управление товарами ====================
  product: {
    title: 'Управление товарами',
    description: 'Управление товарами Ozon, массовое добавление и редактирование',
    addProduct: 'Добавить товар',
    selected: 'Выбрано {count} товаров',
    filter: {
      selectStore: 'Выберите магазин',
      selectStatus: 'Выберите статус',
      selectCategory: 'Выберите категорию',
      searchPlaceholder: 'Поиск по названию или SKU...',
      priceRange: 'Ценовой диапазон',
      min: 'Мин',
      max: 'Макс',
      hasImages: 'С фото',
      reset: 'Сбросить фильтры'
    },
    status: {
      all: 'Все',
      onSale: 'В продаже',
      pending: 'На проверке',
      soldOut: 'Распродан',
      draft: 'Черновик'
    },
    columns: {
      product: 'Товар',
      price: 'Цена',
      stock: 'Остаток',
      status: 'Статус',
      rating: 'Рейтинг',
      updatedAt: 'Обновлено',
      actions: 'Действия'
    },
    batch: {
      publish: 'Опубликовать выбранные',
      unpublish: 'Снять с публикации',
      delete: 'Удалить выбранные',
      publishSuccess: 'Опубликовано {count} товаров',
      unpublishSuccess: 'Снято с публикации {count} товаров',
      deleteSuccess: 'Удалено {count} товаров',
      deleteConfirm: {
        title: 'Подтверждение массового удаления',
        message: 'Вы уверены, что хотите удалить {count} выбранных товаров? Это действие нельзя отменить.'
      }
    },
    detail: {
      title: 'Детали товара',
      basicInfo: 'Основная информация',
      description: 'Описание товара',
      offerId: 'SKU',
      category: 'Категория',
      stock: 'Остаток',
      price: 'Цена'
    },
    deleteConfirm: {
      title: 'Подтверждение удаления',
      message: 'Вы уверены, что хотите удалить товар "{name}"?'
    },
    deleteSuccess: 'Товар удалён',
    errors: {
      loadFailed: 'Не удалось загрузить список товаров'
    }
  },

  // ==================== Верхняя панель ====================
  topbar: {
    deviceManagement: 'Управление устройствами',
    notifications: 'Уведомления',
    markAllRead: 'Все прочитаны',
    profile: 'Профиль',
    settings: 'Настройки',
    help: 'Центр помощи',
    logout: 'Выйти',
    logoutConfirm: 'Вы уверены, что хотите выйти?',
    logoutConfirmTitle: 'Выход из системы',
    logoutSuccess: 'Выход выполнен',
    admin: 'Администратор',
    user: 'Пользователь'
  },

  // ==================== Меню ====================
  menu: {
    chat: 'Чат',
    dashboard: 'Панель управления',
    store: 'Магазины',
    storeManagement: 'Управление магазинами',
    products: 'Товары',
    orders: 'Заказы',
    automation: 'Автоматизация',
    workflows: 'Рабочие процессы',
    scheduledTasks: 'Запланированные задачи',
    tools: 'Инструменты',
    channels: 'Каналы',
    settings: 'Настройки'
  }
} as const
