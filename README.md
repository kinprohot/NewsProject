# News Reading App - Đồ án Cơ sở Ngành 3

Ứng dụng đọc tin tức thông minh tích hợp AI phân tích và tóm tắt nội dung bài báo.

## Thông tin Sinh viên

- **Họ và tên:** Nguyễn Hoàng Lực
- **Lớp:** 23SE2
- **Môn học:** Đồ án Cơ sở Ngành 3
- **Trường:** VKU

## Giới thiệu Dự án

"News Reading App" là một ứng dụng di động Android được xây dựng nhằm mang đến trải nghiệm đọc tin tức hiện đại, cá nhân hóa và hiệu quả cho người dùng. Trong bối cảnh thông tin bùng nổ, ứng dụng giúp người dùng dễ dàng tiếp cận, chọn lọc và tiêu thụ tin tức một cách thông minh.

Điểm nổi bật của ứng dụng là việc tích hợp Trí tuệ Nhân tạo (AI) thông qua Google Gemini API để cung cấp khả năng **phân tích và tóm tắt nội dung bài báo** tự động. Điều này giúp người dùng nhanh chóng nắm bắt ý chính của các bài viết dài mà không cần tốn nhiều thời gian đọc chi tiết.

## Các Tính năng Chính

Ứng dụng cung cấp một loạt các tính năng hữu ích cho người đọc báo:

1.  **Xác thực Người dùng (User Authentication):**
    *   Đăng ký tài khoản mới.
    *   Đăng nhập bằng tài khoản đã có.
2.  **Quản lý Vị trí Người dùng (User Location):**
    *   Người dùng có thể nhập và lưu trữ vị trí của mình.
3.  **Phân loại Tin tức (News Categorization):**
    *   Lọc và duyệt tin tức theo các chủ đề phổ biến như Công nghệ, Kinh tế, Giải trí, Thể thao, v.v.
4.  **Chi tiết Bài báo (News Details):**
    *   Đọc nội dung đầy đủ của bài báo ngay trong ứng dụng.
5.  **Cập nhật Thời gian thực (Real-time Updates):**
    *   Ứng dụng tự động hoặc cho phép người dùng làm mới để lấy các tin tức mới nhất.
6.  **Lưu trữ Yêu thích (Favorites):**
    *   Lưu các bài báo quan tâm để đọc lại sau.
7.  **Tin tức theo Vị trí (Location-Based News):**
    *   Gợi ý các tin tức địa phương dựa trên vị trí người dùng đã cung cấp.
8.  **Chia sẻ Mạng xã hội (Social Sharing):**
    *   Dễ dàng chia sẻ các bài báo thú vị lên các nền tảng mạng xã hội.
9.  **Tìm kiếm Từ khóa (Keyword Search):**
    *   Tìm kiếm bài báo nhanh chóng bằng từ khóa.
10. **Bình luận Bài báo (Article Comments):**
    *   Người dùng có thể đọc và đăng bình luận về các bài báo.
11. **Tích hợp AI (AI Integration - Gemini API):**
    *   **Phân tích nội dung:** (Có thể mở rộng)
    *   **Tóm tắt bài báo:** Cung cấp bản tóm tắt ngắn gọn, súc tích giúp người dùng nắm bắt nhanh thông tin.

## Công nghệ Sử dụng

Dự án được xây dựng với các công nghệ và thư viện hiện đại:

-   **Ngôn ngữ lập trình:** Kotlin
-   **Kiến trúc:** MVVM (Model-View-ViewModel)
-   **Giao diện người dùng (UI):** Jetpack Compose
-   **Quản lý vòng đời & Trạng thái:** Android Jetpack (ViewModel, LiveData/StateFlow, Lifecycle)
-   **Lập trình bất đồng bộ:** Kotlin Coroutines & Flow
-   **Networking:**
    *   Retrofit (HTTP Client)
    *   OkHttp
    *   Gson (JSON Parsing)
-   **Tải ảnh:** Coil (Coroutine Image Loader)
-   **Điều hướng:** Navigation Compose
-   **Lưu trữ cục bộ:** DataStore Preferences
-   **Backend & Dịch vụ đám mây (Firebase):**
    *   Firebase Authentication (Xác thực người dùng)
    *   Firebase Realtime Database (Lưu trữ dữ liệu người dùng, yêu thích, bình luận)
    *   Firebase Analytics (Phân tích hành vi người dùng)
-   **Trí tuệ Nhân tạo (AI):**
    *   Google Gemini API (Phân tích và Tóm tắt văn bản)
-   **Nguồn tin tức:** [Điền tên News API bạn sử dụng, ví dụ: NewsAPI.org, GNews API]

## Cài đặt và Chạy dự án

1.  **Clone repository:**
    ```bash
    git clone https://github.com/kinprohot/NewsProject
    cd [TÊN_THƯ_MỤC_DỰ_ÁN]
    ```
2.  **Mở dự án bằng Android Studio.**
3.  **Cấu hình API Keys:**
    *   Tạo tệp `local.properties` trong thư mục gốc của dự án.
    *   Thêm các API key cần thiết vào tệp này, ví dụ:
        ```properties
        NEWS_API_KEY="YOUR_NEWS_API_KEY"
        GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
        ```
    *   Đảm bảo rằng `build.gradle` đã được cấu hình để đọc các key này.
4.  **Cấu hình Firebase:**
    *   Tạo một dự án Firebase tại [Firebase Console](https://console.firebase.google.com/).
    *   Thêm ứng dụng Android vào dự án Firebase của bạn (sử dụng package name `com.gk.news_pro`).
    *   Tải tệp `google-services.json` từ cài đặt dự án Firebase và đặt nó vào thư mục `app` của dự án Android.
    *   Kích hoạt các dịch vụ Firebase cần thiết (Authentication, Realtime Database, Analytics).
    *   Thiết lập Security Rules cho Realtime Database.
