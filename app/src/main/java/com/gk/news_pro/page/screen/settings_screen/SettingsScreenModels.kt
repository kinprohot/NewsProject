package com.gk.news_pro.page.screen.settings_screen

data class LanguageSettingItem(
    val displayName: String, // Tên hiển thị cho người dùng (ví dụ: "English", "Tiếng Việt")
    val languageCode: String  // Mã ngôn ngữ cho API (ví dụ: "en", "vi")
)

// Danh sách các ngôn ngữ bạn muốn hỗ trợ và hiển thị cho người dùng
// KEY QUAN TRỌNG: languageCode ở đây phải khớp với các giá trị mà API của bạn chấp nhận cho tham số 'language'
val supportedLanguageSettings = listOf(
    LanguageSettingItem("Arabic", "ar"),
    LanguageSettingItem("Dutch", "nl"),
    LanguageSettingItem("French", "fr"),
    LanguageSettingItem("German", "de"),
    LanguageSettingItem("Italian", "it"),
    LanguageSettingItem("Norwegian", "no"),
    LanguageSettingItem("Portuguese", "pt"),
    LanguageSettingItem("Russian", "ru"),
    LanguageSettingItem("Spanish", "es"),
    LanguageSettingItem("Vietnamese", "vi"),
    // Thêm các ngôn ngữ khác mà API của bạn hỗ trợ
)