# 🏇 Horse Racing Management System — Backend

Hệ thống xử lý logic nghiệp vụ, quản lý cơ sở dữ liệu và cung cấp API cho dự án Đua ngựa.  
Được xây dựng trên nền tảng **Java Web**.

---

##  Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Language | Java (JDK 17+) |
| Build Tool | Maven |
| Database | SQL Server |
| Libraries | JDBC / JPA (Lombok, JUnit 5) |
| Architecture | MVC / Layered Architecture |

---

##  Hướng dẫn thiết lập

### 1. Cấu hình cơ sở dữ liệu

1. Mở **SQL Server Management Studio (SSMS)**.
2. Chạy file `/database/schema.sql` để tạo các bảng.
3. Chạy file `/database/data.sql` để nạp dữ liệu mẫu.

### 2. Cấu hình kết nối

Mở file `src/main/resources/db.properties` và cập nhật thông số sau:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=HorseRacingDB
db.user=your_username
db.password=your_password
```

### 3. Cài đặt & Chạy dự án

```bash
# Cài đặt thư viện
mvn clean install

# Chạy Unit Test (bắt buộc trước khi push code)
mvn test

# Khởi động server
mvn spring-boot:run
```

---

##  Cấu trúc thư mục

```
src/main/java/com/racing/
├── model/       # Entities ánh xạ từ Database
├── dao/         # Data Access Object — chứa các câu lệnh SQL
├── dto/         # Data Transfer Object — dữ liệu trả về cho Frontend
├── service/     # Xử lý logic nghiệp vụ chính
└── controller/  # Tiếp nhận Request và trả về JSON API
```

---

##  Quy tắc code (Convention)

### Đặt tên

| Loại | Quy tắc | Ví dụ |
|---|---|---|
| Class | `PascalCase` | `HorseService.java`, `UserDAO.java` |
| Method / Variable | `camelCase` | `getHorseById`, `isActive` |

### DAO & DTO

-  **Tuyệt đối không** trả về trực tiếp lớp Model cho Frontend (tránh lộ thông tin nhạy cảm).
-  Mọi dữ liệu trả về qua API **phải được đóng gói vào DTO**.

### Unit Testing

- Mọi hàm xử lý logic tại lớp **Service** và **DAO** đều phải có file Test tương ứng trong `src/test`.

### Git Flow

-  Nhánh chính: `main` _(Stable)_ · `develop` _(Working)_
-  **Tuyệt đối không** push trực tiếp lên `main` hoặc `develop`.
-  Tạo nhánh tính năng theo cú pháp: `feature/be-ten-tinh-nang`
-  Tạo **Pull Request (PR)** và chờ Leader review trước khi Merge.

---

##  Thành viên Backend

| Tên | Vai trò |
|---|---|
| Tạ Vũ Hảo (Leader) | Backend Developer, DevOps |
| Nguyễn Hồng Duy | Backend Architect & Database Design |


