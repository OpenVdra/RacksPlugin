---
title: Lệnh
---

# Lệnh

Mọi lệnh đều bắt đầu bằng `/racks`. `/rack` cho kết quả giống hệt.

Người chơi thường không cần lệnh nào. Giá treo được chế tạo, đặt và sử dụng hoàn toàn trong thế giới.

<BaseTable :columns="['Lệnh', 'Tác dụng']" grid="1fr 1fr">

<CommandRow commands="/racks give &lt;gỗ&gt; [người chơi] [số lượng]" permission="racks.command.give">

Phát một giá treo. Không ghi tên người chơi thì phát cho chính mình. Loại gỗ là một trong mười hai
[tên loại gỗ](/vi/docs/wood-variants#danh-sach-day-du), và số lượng từ 1 đến 64.

Phần không vừa túi đồ của người nhận sẽ rơi xuống chân họ.

</CommandRow>

<CommandRow commands="/racks reload" permission="racks.command.reload">

Đọc lại `config.yml` và các file ngôn ngữ. Giá treo đã có trong thế giới không bị ảnh hưởng.

Dùng lệnh này sau khi sửa bất kỳ file nào trong `plugins/Racks/`.

</CommandRow>

</BaseTable>

## Ví dụ

Tự phát cho mình một giá treo gỗ sồi:

```
/racks give oak
```

Phát cho người chơi khác mười sáu giá treo gỗ anh đào:

```
/racks give cherry Steve 16
```

Áp dụng thay đổi vừa sửa trong `config.yml`:

```
/racks reload
```
