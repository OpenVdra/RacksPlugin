---
title: Cài đặt
---

# Cài đặt

Racks thêm giá treo bằng gỗ để trưng bày công cụ và vũ khí. Chế tạo một cái, đặt xuống sàn hoặc gắn
lên tường, rồi chuột phải với vật phẩm trên tay.

Đây là bản chuyển thành plugin của
[data pack Racks by KawaMood](https://modrinth.com/datapack/racks). Mọi thứ người chơi thấy đều giữ
nguyên. Khác biệt nằm ở bên dưới: giá treo được lưu trong cơ sở dữ liệu, nên chúng vẫn còn sau khi
nâng cấp thế giới.

## Yêu cầu

- Minecraft 1.21.11
- Paper, một bản fork của Paper, hoặc Folia
- Java 21

Không cần resource pack. Vật phẩm giá treo là đầu người chơi với skin riêng, còn bản thân giá treo
được dựng từ hàng rào và nút bấm thông thường, nên nó hiển thị đúng trên client gốc.

## Tải về

Lấy bản `Racks-<phiên bản>.jar` mới nhất từ kênh phân phối chính thức của dự án.

## Cách cài

1. Tắt máy chủ.
2. Nếu data pack Racks đang có trong bất kỳ thế giới nào, hãy gỡ nó ngay bây giờ. Đọc
   [Chuyển từ data pack](/vi/docs/migration) trước khi làm.
3. Bỏ file jar vào thư mục `plugins`.
4. Bật máy chủ.

Lần khởi động đầu tiên, plugin tạo thư mục `plugins/Racks/` gồm `config.yml`, các file ngôn ngữ và
một file `racks.db` trống.

## Chiếc giá treo đầu tiên

<CardGrid>
<FeatureCard icon="Hammer" title="1. Chế tạo">

Ba tấm ván xếp một hàng, hai que gậy phía dưới hai đầu. Dùng loại gỗ nào trong mười hai loại cũng được.

</FeatureCard>
<FeatureCard icon="Blocks" title="2. Đặt xuống">

Xuống sàn để có hai ô, hoặc áp vào tường để có một ô. Giá đặt sàn sẽ tự quay về phía bạn.

</FeatureCard>
<FeatureCard icon="Hand" title="3. Treo đồ">

Chuột phải khi cầm công cụ để treo lên. Chuột phải với tay không để lấy xuống.

</FeatureCard>
</CardGrid>

## Đọc tiếp

<CardGrid>
<DocCard icon="Axe" title="Cách dùng giá treo" link="/vi/docs/using-racks" desc="Chế tạo, đặt, đổi tư thế, và những gì giá treo nhận hoặc không nhận." />
<DocCard icon="TreePine" title="Các loại gỗ" link="/vi/docs/wood-variants" desc="Cả mười hai loại gỗ và hình dáng của chúng." />
<DocCard icon="Settings" title="Cấu hình" link="/vi/docs/configuration" desc="Từng tùy chọn trong config.yml và tác dụng khi thay đổi." />
<DocCard icon="ArrowRightLeft" title="Chuyển từ data pack" link="/vi/docs/migration" desc="Giữ lại những giá treo đã có sẵn trong thế giới." />
</CardGrid>
