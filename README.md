# Clone Proxy Manager — prototype

Ứng dụng Android mẫu để:
- Tạo hồ sơ quản lý.
- Lưu cấu hình Proxy HTTP/HTTPS (host/port).
- Gán một Proxy cho từng hồ sơ.
- Kiểm tra kết nối HTTPS thông qua Proxy và hiển thị IP thoát.
- Lưu cấu hình cục bộ trên thiết bị.

## Giới hạn quan trọng

Đây là **ứng dụng quản lý cấu hình và kiểm tra Proxy**, chưa tích hợp vào runtime của `Clone App Pro_VIP_FixV3.apk` và **không tự định tuyến lưu lượng của các bản sao ảo**. Việc gán Proxy trong UI chỉ lưu lựa chọn cho hồ sơ.

Để áp dụng Proxy thực sự cho lưu lượng của một bản sao, cần quyền và điểm tích hợp tại tầng mạng của trình nhân bản. Nếu các bản sao cùng chạy trong một tiến trình/UID của ứng dụng quản lý, `VpnService` tiêu chuẩn không tự tách được từng bản sao theo profile ID.

## Build

1. Mở thư mục này bằng Android Studio (bản hỗ trợ Android Gradle Plugin 8.7.3).
2. Cài Android SDK Platform 35.
3. Đồng bộ Gradle.
4. Chạy trên thiết bị Android hoặc dùng `Build > Build APK(s)`.

Ứng dụng cần quyền INTERNET để kiểm tra Proxy. Nên chỉ kiểm tra với Proxy bạn sở hữu hoặc được phép sử dụng.

## Bảo mật và giới hạn kiểm tra

- Prototype hiện không hỗ trợ Proxy có username/password.
- Dữ liệu cấu hình lưu trong SharedPreferences; không lưu mật khẩu.
- Kiểm tra sử dụng `https://api.ipify.org` qua Java HTTP proxy. Máy chủ Proxy cần hỗ trợ HTTP CONNECT cho HTTPS.
- Không sử dụng kết quả kiểm tra này làm bằng chứng rằng các bản sao bên trong ứng dụng khác đã đi qua Proxy.
- Không có chức năng sửa IMEI/serial phần cứng.


## Build APK in GitHub Actions (without Android Studio locally)

1. Create a GitHub repository and upload the contents of this project.
2. Push to the `main` branch, or open **Actions** and run **Build Android APK** using **Run workflow**.
3. Open the completed workflow run and download the `CloneProxyManager-debug-apk` artifact.
4. The artifact contains `app-debug.apk`.

This creates a debug APK for testing. It does not integrate the manager with another cloning app's internal network runtime.
