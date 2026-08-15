---
title: Cách dùng giá treo
---

# Cách dùng giá treo

## Chế tạo

Ba tấm ván xếp một hàng, hai que gậy phía dưới hai đầu. Mỗi lần chế tạo được một giá treo.

![Công thức chế tạo giá treo](/media/recipe.gif)

Loại gỗ nào trong mười hai loại cũng được, và giá treo sẽ mang đúng loại gỗ bạn dùng. Xem
[Các loại gỗ](/vi/docs/wood-variants).

Người vận hành cũng có thể phát giá treo bằng [`/racks give`](/vi/docs/commands).

## Đặt giá treo

Đặt xuống sàn thì giá treo được **hai** công cụ. Gắn vào mặt bên của một khối thì được **một**.

Giá đặt sàn tự quay về phía người đặt. Giá treo tường quay ra ngoài, ngược hướng bức tường đỡ nó.

::: tip
Hãy nhắm vào khối mà bạn muốn giá treo nằm trước mặt, đừng nhắm xuống sàn bên cạnh. Giá treo được
đặt giống mọi khối khác.
:::

## Treo đồ lên và lấy xuống

**Chuột phải** vào giá treo khi đang cầm vật phẩm ở tay chính để treo lên.
**Chuột phải** với tay không để lấy xuống.

Giá đặt sàn có vùng bấm riêng cho từng ô trong hai ô, nên bạn đổi được một công cụ mà không đụng tới
công cụ còn lại.

Nếu ô trên giá đã có đồ mà tay bạn đang cầm công cụ khác, một cú chuột phải sẽ đổi chỗ hai món.

<Gallery :items="[
  { src: '/media/variants-overworld-empty.png', caption: 'Giá treo còn trống' },
  { src: '/media/variants-overworld.png', caption: 'Cũng những giá đó khi đã treo công cụ' }
]" />

## Đổi tư thế {#doi-tu-the}

**Cúi người và chuột phải** để đổi cách vật phẩm nằm trên giá. Giá đặt sàn có sáu kiểu, giá treo
tường có bốn kiểu. Bấm tiếp sẽ quay lại kiểu đầu tiên.

Giá treo trống thì không có gì để xoay, nên cúi người bấm vào sẽ không xảy ra chuyện gì.

## Phá giá treo

**Chuột trái** để phá. Giá treo rơi ra thành vật phẩm, và mọi thứ đang nằm trên nó cũng rơi theo.

Vật phẩm trên giá luôn rơi, kể cả khi người phá đang ở chế độ sáng tạo. Riêng bản thân giá treo thì
không rơi trong chế độ sáng tạo.

Giá treo tường cũng tự rơi nếu khối đỡ nó bị phá, giống như bức tranh. Xem tùy chọn
`ignore-wall-rack-support` trong [Cấu hình](/vi/docs/configuration) để tắt điều đó.

## Những gì đặt được lên giá {#nhung-gi-dat-duoc-len-gia}

Giá treo dành cho công cụ và vũ khí, không dành cho khối hay nguyên liệu. Giá treo tường nhận nhiều
loại hơn giá đặt sàn.

<CardGrid>
<FeatureCard icon="Pickaxe" title="Giá đặt sàn">

Rìu, cuốc, cúp, xẻng, kiếm, giáo, chùy, cần câu, cà rốt gắn cần và nấm cong vênh gắn cần.

</FeatureCard>
<FeatureCard icon="Shield" title="Giá treo tường">

Mọi thứ giá đặt sàn nhận, cộng thêm cung, nỏ, đinh ba, khiên, kéo và kính viễn vọng.

</FeatureCard>
</CardGrid>

Chuột phải bằng vật phẩm khác sẽ không có tác dụng gì. Giá treo không nhận vật phẩm đó và cũng không
tự bỏ trống.

Phù phép, tên tùy chỉnh, hoa văn, độ bền và vật phẩm do plugin khác thêm vào đều trở lại đúng như
lúc được treo lên.

::: warning Vật phẩm đổi texture bằng resource pack
Giá treo hiển thị mô hình gốc của vật phẩm. Nếu một resource pack đổi mô hình của công cụ hoặc hướng
của texture, món đồ có thể nằm hơi lệch trên giá. Data pack gốc cũng vậy.
:::
