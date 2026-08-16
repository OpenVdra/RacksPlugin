---
title: Cấu hình
---

# Cấu hình

Toàn bộ nằm trong `plugins/Racks/config.yml`. Chữ hiển thị cho người chơi không nằm ở đây mà ở
[các file ngôn ngữ](/vi/docs/language).

Thay đổi có hiệu lực khi chạy `/racks reload`, trừ những chỗ có ghi chú riêng.

## Các tùy chọn

<ConfigProperty name="language" value="en_US">

Ngôn ngữ dùng cho người chơi mà ngôn ngữ của họ chưa có file, và cho những nội dung không có người
đọc cụ thể, chẳng hạn một giá treo đang nằm dưới đất.

Phải trùng tên một thư mục trong `plugins/Racks/language/`. Plugin có sẵn `en_US` và `vi_VN`.

</ConfigProperty>

<ConfigProperty name="language-auto-detect" value="true" type="boolean">

Hiển thị tin nhắn theo ngôn ngữ game của từng người chơi. Chỉ những ngôn ngữ có thư mục trong
`language/` mới hiện được, số còn lại dùng tùy chọn `language` phía trên.

Tắt đi thì mọi người dùng chung một ngôn ngữ, giống cách data pack gốc hoạt động. Tắt cũng làm mọi
giá treo cùng loại gỗ xếp chồng được, vì khi đó chúng có tên giống hệt nhau.

</ConfigProperty>

<ConfigGroup name="database">
<ConfigProperty name="file" value="racks.db">

File cơ sở dữ liệu nằm trong `plugins/Racks/`. Nó chứa mọi giá treo đã đặt và những gì đang nằm trên
chúng.

**Cần khởi động lại máy chủ.** Hãy sao lưu file này cùng với thế giới. Mất nó nghĩa là mọi giá treo
trong thế giới ngừng hoạt động, và vật phẩm trên chúng cũng mất theo.

</ConfigProperty>
</ConfigGroup>

<ConfigGroup name="settings">
<ConfigProperty name="ignore-wall-rack-support" value="false" type="boolean">

Giá treo tường có trụ lại được khi mất khối đỡ hay không.

`false` giống data pack: giá treo tự rơi cùng vật phẩm trên nó, như một bức tranh. `true` để nó lơ
lửng, và ngừng luôn việc kiểm tra bên dưới.

</ConfigProperty>
<ConfigProperty name="wall-support-check-interval" value="10" type="number">

Bao lâu giá treo tường kiểm tra khối phía sau một lần, tính bằng tick. 20 tick là một giây.

Đặt cao hơn thì máy chủ làm việc ít hơn và giá treo mất nhiều thời gian hơn mới rơi. Tùy chọn này
không có tác dụng khi `ignore-wall-rack-support` đang bật.

</ConfigProperty>
</ConfigGroup>

<ConfigGroup name="protection">
<ConfigProperty name="worldguard" value="true" type="boolean">

Kiểm tra vùng WorldGuard trước khi người chơi phá giá treo hoặc đổi vật phẩm trên đó. Phá cần cờ
`block-break`, đổi và xoay cần cờ `interact`.

Bị bỏ qua khi chưa cài WorldGuard. Tắt nó đi không ảnh hưởng tới GriefPrevention bên dưới. Xem
[Plugin bảo vệ đất](/vi/docs/protections).

</ConfigProperty>
<ConfigProperty name="griefprevention" value="true" type="boolean">

Kiểm tra mảnh đất GriefPrevention trước khi người chơi phá giá treo hoặc đổi vật phẩm trên đó. Phá
cần mức tin cậy Build, đổi và xoay cần mức Container.

Bị bỏ qua khi chưa cài GriefPrevention. Tắt nó đi không ảnh hưởng tới WorldGuard phía trên. Xem
[Plugin bảo vệ đất](/vi/docs/protections).

</ConfigProperty>
</ConfigGroup>

<ConfigProperty name="adopt-datapack-racks" value="false" type="boolean">

Nhập những giá treo do data pack Racks để lại trong thế giới, theo từng chunk khi chunk được tải.

Hãy gỡ data pack trước khi bật máy chủ, nếu không cả hai sẽ tranh nhau cùng những giá treo đó. Xem
[Chuyển từ data pack](/vi/docs/migration).

</ConfigProperty>
<ConfigProperty name="recipes-enabled" value="true" type="boolean">

Đăng ký mười hai công thức chế tạo.

Tắt đi thì không chế tạo được giá treo, và giá treo chỉ đến từ `/racks give` hoặc bảng loot do chủ
máy chủ tự thêm.

</ConfigProperty>
<ConfigProperty name="update-checker" value="true" type="boolean">

Tra bản phát hành mới nhất một lần lúc khởi động, rồi báo cho người vận hành trong chat và trong
console nếu có bản mới hơn.

**Cần khởi động lại máy chủ.** Plugin không tải hay cài gì cả, chỉ đọc số phiên bản. Modrinth được
hỏi trước, nếu không kết nối được thì dùng trang phát hành trên GitHub. Tắt đi thì bỏ hẳn lượt tra
này, phù hợp với máy chủ không có kết nối ra ngoài.

</ConfigProperty>

## Toàn bộ file

```yaml
language: en_US
language-auto-detect: true

database:
  file: racks.db

settings:
  ignore-wall-rack-support: false
  wall-support-check-interval: 10

protection:
  worldguard: true
  griefprevention: true

adopt-datapack-racks: false
recipes-enabled: true
update-checker: true
```
