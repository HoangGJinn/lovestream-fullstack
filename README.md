# 🎬 LoveStream - Premium Movie Streaming Platform

LoveStream là một nền tảng xem phim trực tuyến hiện đại được xây dựng nhằm mang lại trải nghiệm giải trí mượt mà. Dự án được phát triển dựa trên kiến trúc Microservices định hướng, sử dụng Java 21 và Spring Boot 3.5.

## 🚀 Tech Stack

- **Backend:** Java 21, Spring Boot 3.5.11, Spring Security (JWT), Spring Data JPA.
- **Frontend:** Thymeleaf.
- **Database:** MySQL 8.0.
- **Tools:** Maven, Docker, Git.

## 🛠 Quy trình phát triển (Workflow)

Dự án tuân thủ quy trình **Git Flow** cơ bản để đảm bảo tính ổn định của mã nguồn.

### 1. Cấu trúc Nhánh (Branches)
- `main`: Nhánh chính thức, chứa code ổn định nhất (Production-ready).
- `dev`: Nhánh phát triển chính. Mọi tính năng mới đều được gộp vào đây trước khi lên main.
- `feature/name`: Nhánh riêng cho từng tính năng (Ví dụ: `feature/login`, `feature/payment`).

### 2. Quy tắc Commit (Conventional Commits)
Sử dụng cấu trúc: `<type>: <description>`
- `feat`: Tính năng mới.
- `fix`: Sửa lỗi.
- `docs`: Cập nhật tài liệu/README.
- `refactor`: Tối ưu hóa code nhưng không thay đổi tính năng.

## ⚙️ Cài đặt nhanh

1. **Clone dự án:**
   ```bash
   git clone [https://github.com/HoangGJinn/lovestream-fullstack.git](https://github.com/HoangGJinn/lovestream-fullstack.git)