# Fix F12 (Go to Definition) không hoạt động trong Java project

## Nguyên nhân
Java Language Server chưa đồng bộ hoàn toàn với Maven multi-module project của SpringCRM.

## Giải pháp nhanh (3 bước)

### Bước 1: Reload Window
Nhấn `Ctrl+Shift+P` → gõ `Developer: Reload Window` → Enter

### Bước 2: Clean Java Workspace (nếu vẫn chưa fix)
1. Nhấn `Ctrl+Shift+P`
2. Gõ `Java: Clean Java Language Server Workspace`
3. Chọn `Restart and delete` → Yes

### Bước 3: Force Maven Reimport (nếu vẫn chưa fix)
1. Mở Command Palette: `Ctrl+Shift+P`
2. Gõ `Java: Force Java Compilation`
3. Hoặc `Java: Reload Projects`

## Kiểm tra
Mở file `JwtTokenProvider.java` → đặt con trỏ vào `SecurityConstants` dòng 4 hoặc 37 → nhấn **F12**.

- ✅ Nếu nhảy đến `SecurityConstants.java` trong `shared-lib` → **đã fix xong**
- ❌ Nếu vẫn không nhảy → tiếp tục bước dưới

## Bước khắc phục nâng cao

### Kiểm tra Java Extension
1. Đảm bảo extension `Extension Pack for Java` (vscjava.vscode-java-pack) đã được cài
2. Đảm bảo `Maven for Java` extension được bật

### Force Clean + Recompile
Mở terminal trong VSCode và chạy:

```bash
cd backend
mvn clean compile -DskipTests
```

Sau đó reload window lại.

## Cấu hình đã thêm vào project

### `.vscode/settings.json`

**Chế độ IntelliSense** (null analysis bật — nhiều cảnh báo từ JDT):
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic",
  "java.import.maven.enabled": true
}
```

**Chế độ Option A** (tin Maven + SpotBugs, tắt cảnh báo null JDT — chuẩn enterprise):
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "disabled",
  "java.import.maven.enabled": true,
  "java.errors.incompleteClasspath.severity": "ignore"
}
```

Project mặc định dùng **Option A**. Nguồn sự thật cho null/bug: `mvn verify` (compiler -Werror + SpotBugs).

### `.vscode/extensions.json`
```json
{
  "recommendations": [
    "vscjava.vscode-java-pack",
    "vscjava.vscode-maven",
    "vmware.vscode-boot-dev-pack"
  ]
}
```

## Lưu ý quan trọng

- Nếu bạn đang dùng **Cursor**, chức năng Java IntelliSense dựa trên VSCode Java extension nên các bước trên vẫn áp dụng.
- Sau khi reload window lần đầu, Java Language Server cần 10-30 giây để build và index toàn bộ workspace.
- Quan sát thanh progress bar dưới cùng IDE (hiển thị "Building workspace...").

---

**TL;DR: Chạy ngay `Ctrl+Shift+P` → `Developer: Reload Window` là đủ trong 90% trường hợp.**
