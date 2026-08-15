---
title: Ngôn ngữ
---

# Ngôn ngữ

Plugin có sẵn tiếng Anh và tiếng Việt. Mỗi người chơi đọc tin nhắn bằng ngôn ngữ game của mình, nếu
ngôn ngữ đó có file.

## Chữ nằm ở đâu

```
plugins/Racks/language/
  en_US/messages.yml
  vi_VN/messages.yml
```

Cả hai file được tạo ở lần khởi động đầu tiên. Sửa file rồi chạy `/racks reload`.

## Người chơi thấy ngôn ngữ nào

Plugin tìm ngôn ngữ game của người chơi trước. Nếu không có thư mục cho ngôn ngữ đó, nó lùi về tùy
chọn `language` trong `config.yml`, rồi cuối cùng là `en_US`, vốn luôn có sẵn.

Chỉ những ngôn ngữ có thư mục trong `language/` mới hiển thị được. Thêm thư mục là đủ.

Đặt `language-auto-detect` thành `false` trong `config.yml` để mọi người dùng chung tùy chọn
`language`, bất kể ngôn ngữ game của họ.

## Thêm một ngôn ngữ

1. Sao chép `language/en_US/` thành `language/<mã ngôn ngữ>/`, ví dụ `language/de_DE/`.
2. Dịch phần giá trị. Giữ nguyên phần khóa và các `{placeholder}`.
3. Chạy `/racks reload`.

Không cần cập nhật plugin, và thư mục đó sẽ được đọc trong mọi lần reload về sau.

## Cách viết nội dung

Màu sắc dùng được theo hai cách. Thẻ kiểu mới:

```yaml
reloaded: '{prefix}<gray>Đã tải lại cấu hình và các tệp ngôn ngữ.'
```

Hoặc mã `&` kiểu cũ, gồm cả `&#RRGGBB` cho màu hex:

```yaml
reloaded: '{prefix}&7Đã tải lại cấu hình và các tệp ngôn ngữ.'
```

`{prefix}` chèn giá trị `prefix` ở đầu chính file đó. Các `{placeholder}` còn lại do plugin điền vào,
nên hãy giữ đủ những cái xuất hiện trong dòng tiếng Anh.

## Tên vật phẩm

Giá treo mang tên theo ngôn ngữ của người nhận nó, vì tên nằm sẵn trong vật phẩm chứ không được đọc
lại cho từng người xem.

Trên máy chủ có nhiều ngôn ngữ, điều đó nghĩa là hai giá treo cùng loại gỗ có thể không xếp chồng
được. Đặt `language-auto-detect` thành `false` nếu bạn muốn chúng luôn xếp chồng.
