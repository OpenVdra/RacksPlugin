---
title: Chuyển từ data pack
---

# Chuyển từ data pack

Những giá treo đã có sẵn trong thế giới có thể được nhập tự động, kèm theo vật phẩm đang nằm trên
chúng — bật `adopt-datapack-racks` trong `config.yml` (mặc định tắt).

::: warning Gỡ data pack trước
Đừng chạy data pack và plugin cùng lúc. Cả hai sẽ tranh nhau điều khiển cùng những giá treo đó.
:::

## Các bước

1. Tắt máy chủ.
2. Sao lưu thế giới. Đây là bước đáng làm nhất.
3. Xóa data pack Racks khỏi thư mục `datapacks` của mọi thế giới. **Đừng** chạy hàm gỡ cài đặt của
   data pack, vì hàm đó xóa luôn các giá treo.
4. Bỏ file jar của plugin vào `plugins`.
5. Trong `plugins/Racks/config.yml`, đặt `adopt-datapack-racks: true`.
6. Bật máy chủ.

Vậy là xong. Giá treo được nhập theo từng chunk khi chunk được tải, nên một thế giới lớn sẽ chuyển
dần theo bước chân người chơi chứ không dồn hết vào lúc khởi động.

Để nguyên `adopt-datapack-racks: false` (mặc định) nếu bạn muốn giữ nguyên các giá treo cũ của data
pack, không đưa chúng vào plugin.

## Những gì được giữ lại

<CardGrid>
<FeatureCard icon="Check" title="Giữ lại">

Loại gỗ, hướng quay của giá treo, giá đặt sàn hay treo tường, vật phẩm trên đó và tư thế chúng đang
nằm.

</FeatureCard>
<FeatureCard icon="TriangleAlert" title="Không giữ">

Người đã đặt giá treo ban đầu. Data pack có ghi lại nhưng chưa bao giờ dùng tới, và plugin cũng vậy.

</FeatureCard>
</CardGrid>

Giá treo được nhập luôn được xem là đủ cũ để rơi khi bị phá, bất kể `lootable-delay` đặt bao nhiêu.
Data pack cũng đối xử với các giá treo đời cũ của nó như vậy.

## Vật phẩm giá treo trong túi đồ người chơi

Không cần làm gì. Giá treo nằm trong rương, shulker và túi đồ vẫn dùng được ngay.

Chúng được làm mới sang dạng của plugin trong lần đăng nhập kế tiếp của chủ sở hữu, và nhờ đó có tên
theo ngôn ngữ của người chơi thay vì tiếng Anh cố định.

## Nếu một giá treo biến mất sau khi chuyển

Hãy đi vào chunk chứa nó, đi ra rồi quay lại. Giá treo được dựng lại từ cơ sở dữ liệu mỗi khi chunk
được tải mà phát hiện thiếu, nên một vòng đi về thường là đủ.

## Chuyển từ Racks V.2

Hãy cập nhật lên data pack V.3 và chạy hàm nâng cấp `from_v2` của nó **trước khi** cài plugin.
Plugin chỉ đọc được giá treo V.3.
