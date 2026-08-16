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

Hiển thị tin nhắn theo ngôn ngữ game của từng người chơi, nếu ngôn ngữ đó có thư mục.

Tắt đi thì mọi người dùng chung tùy chọn `language` phía trên, giống cách data pack gốc hoạt động.
Tắt cũng làm mọi giá treo cùng loại gỗ xếp chồng được, vì khi đó chúng có tên giống hệt nhau.

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

`false` là mặc định và giống data pack: giá treo tự rơi cùng vật phẩm trên nó, như một bức tranh.
`true` để nó lơ lửng, và ngừng luôn việc kiểm tra.

`/racks setting ignore-wall-rack-support <true|false>` đổi được ngay trong game và ghi lại vào đây.

</ConfigProperty>
<ConfigProperty name="wall-support-check-interval" value="10" type="number">

Bao lâu giá treo tường kiểm tra khối phía sau một lần, tính bằng tick. 20 tick là một giây.

Tăng lên thì máy chủ làm việc ít hơn và giá treo mất nhiều thời gian hơn mới rơi. Tùy chọn này không
có tác dụng khi `ignore-wall-rack-support` đang bật.

</ConfigProperty>
<ConfigProperty name="lootable-delay" value="0" type="number">

Giá treo phải đứng bao lâu, tính bằng tick, thì khi phá mới rơi ra vật phẩm giá treo. `0` là luôn
rơi.

Hãy tăng lên nếu plugin bảo vệ đất của bạn hủy việc đặt khối trễ một nhịp, vì điều đó cho phép người
chơi đặt rồi phá để nhân đôi giá treo. `40` là hai giây và đã quá đủ.

Vật phẩm đang nằm trên giá thì luôn rơi, bất kể đặt giá trị nào.

</ConfigProperty>
</ConfigGroup>

<ConfigGroup name="protection">
<ConfigProperty name="worldguard" value="true" type="boolean">

Hỏi vùng WorldGuard trước khi cho người chơi phá giá treo hoặc đổi vật phẩm trên đó, khi WorldGuard
đang cài. Không có tác dụng nếu chưa cài. Xem [Plugin bảo vệ đất](/vi/docs/protections) để biết mỗi
hành động kiểm tra cờ nào.

Tắt đi thì chỉ bỏ qua riêng phần kiểm tra của WorldGuard, độc lập với `griefprevention` bên dưới.
Không ảnh hưởng đến việc đặt giá treo — đó là đặt block bình thường, đã được WorldGuard tự bảo vệ sẵn.

</ConfigProperty>
<ConfigProperty name="griefprevention" value="true" type="boolean">

Hỏi mảnh đất GriefPrevention trước khi cho người chơi phá giá treo hoặc đổi vật phẩm trên đó, khi
GriefPrevention đang cài. Không có tác dụng nếu chưa cài. Xem
[Plugin bảo vệ đất](/vi/docs/protections) để biết mỗi hành động cần mức tin cậy nào.

Tắt đi thì chỉ bỏ qua riêng phần kiểm tra của GriefPrevention, độc lập với `worldguard` phía trên.
Không ảnh hưởng đến việc đặt giá treo — đó là đặt block bình thường, đã được GriefPrevention tự bảo
vệ sẵn.

</ConfigProperty>
</ConfigGroup>

<ConfigProperty name="adopt-datapack-racks" value="false" type="boolean">

Nhập những giá treo do data pack Racks để lại trong thế giới, theo từng chunk khi chunk được tải.

Hãy gỡ data pack trước khi bật máy chủ, nếu không cả hai sẽ tranh nhau cùng những giá treo đó. Xem
[Chuyển từ data pack](/vi/docs/migration).

</ConfigProperty>
<ConfigProperty name="recipes-enabled" value="true" type="boolean">

Đăng ký mười hai công thức chế tạo.

Tắt đi thì người chơi không chế tạo được giá treo, và giá treo chỉ đến từ `/racks give` hoặc bảng
loot do bạn tự thêm.

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
  lootable-delay: 0

protection:
  worldguard: true
  griefprevention: true

adopt-datapack-racks: false
recipes-enabled: true
```
