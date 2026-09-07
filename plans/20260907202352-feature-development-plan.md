# AppXCode feature development plan

Mục tiêu: xây dựng IDE IntelliJ Platform, ưu tiên macOS, khôi phục workflow AppCode cho iOS, iPadOS, macOS, watchOS, tvOS; mở rộng Linux/Windows qua macOS build agent.

## Các phase

1. **Base chạy được:** plugin IntelliJ, branding AX, Gradle/Java, CI, service registry, health check và Plugin Verifier.
2. **Workspace/project model:** đọc trực tiếp `.xcodeproj`/`.xcworkspace`, schemes, targets, configurations; Project view và file watcher.
3. **Swift/Objective-C intelligence:** parser/indexer, completion, diagnostics, navigation, formatter và refactoring dựa trên SourceKit/LSP.
4. **Dependency/Git:** SwiftPM, CocoaPods, resolve/update có log/cache; Git diff, merge, commit, branch, blame và hooks.
5. **Build/run/test/debug:** xcodebuild matrix, signing, archive/export; XCTest, Quick, Kiwi, Catch; LLDB breakpoint và console.
6. **Thiết bị Apple:** Embedded Devices cho Simulator và máy thật; pairing, install/launch, logs, screenshots, test destination; vphone-cli là adapter experimental, tắt mặc định.
7. **Flutter profile:** Dart/Flutter plugin tương thích, pub, hot reload, DevTools, runner iOS/macOS và dùng chung device pipeline.
8. **Remote Linux/Windows:** IDE/editor chạy trên Linux/Windows, build/sign/test/device qua macOS agent với protocol versioned, auth, artifact transfer, retry/cancel.
9. **Parity/release:** benchmark AppCode workflows, startup/indexing/completion, crash diagnostics, compatibility matrix, signed distribution và rollback.

## Quy tắc cho mỗi lượt

Chọn một vertical slice nhỏ, viết interface trước, triển khai, test tự động, kiểm tra thủ công, review độc lập, sau đó commit và push ngay lên `main`. Chỉ chuyển phase khi acceptance gate xanh.

## Acceptance chính

- Mở và chỉnh sửa project Xcode mà không chuyển đổi hay làm mất dữ liệu.
- Completion/navigation/refactoring Swift hoạt động trên project mẫu nhiều target.
- Resolve dependency, build, archive, test và debug được trên Simulator.
- Máy thật cắm USB có thể install/run/test; vphone chỉ hiện khi capability hợp lệ.
- Flutter iOS/macOS có hot reload và debug.
- Linux/Windows gọi build/test trên Mac và nhận artifact/log an toàn.

## Rủi ro và giới hạn

SourceKit, Xcode và signing phụ thuộc phiên bản macOS nên cần capability detection và ma trận toolchain. Plugin IntelliJ proprietary chỉ được cam kết sau verifier/test. vphone-cli là công cụ ngoài, phải cô lập và không tự thay đổi SIP/AMFI/firmware. Native iOS build trên Linux/Windows vẫn cần Mac agent.
