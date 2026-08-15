---
title: Các loại gỗ
---

# Các loại gỗ

Giá treo có mười hai loại gỗ. Công thức giống nhau cho tất cả, loại ván bạn dùng quyết định loại giá
treo bạn nhận được.

<Gallery :items="[
  { src: '/media/variants-overworld-empty.png', caption: 'Bạch dương, sồi, nhiệt đới, vân sam, sồi sẫm' },
  { src: '/media/variants-overworld.png', caption: 'Cũng năm loại đó khi đã treo công cụ' },
  { src: '/media/variants-nether-empty.png', caption: 'Tre, keo, đước, anh đào, đỏ thẫm, cong vênh' },
  { src: '/media/variants-nether.png', caption: 'Cũng sáu loại đó khi đã treo công cụ' }
]" />

## Danh sách đầy đủ {#danh-sach-day-du}

Dùng các tên này với [`/racks give`](/vi/docs/commands):

`acacia` &nbsp; `bamboo` &nbsp; `birch` &nbsp; `cherry` &nbsp; `crimson` &nbsp; `dark_oak`
&nbsp; `jungle` &nbsp; `mangrove` &nbsp; `oak` &nbsp; `pale_oak` &nbsp; `spruce` &nbsp; `warped`

Sồi nhạt được thêm vào sau khi các ảnh phía trên được chụp.

## Giá treo khác loại gỗ thì không xếp chồng

Đó là chuyện bình thường. Mỗi loại gỗ là một vật phẩm riêng.

Trên máy chủ có người chơi dùng nhiều ngôn ngữ khác nhau, hai giá treo *cùng* một loại gỗ cũng có
thể không xếp chồng được, vì mỗi cái mang tên theo ngôn ngữ của người nhận nó. Đặt
`language-auto-detect` thành `false` trong `config.yml` nếu bạn muốn chúng luôn xếp chồng. Xem
[Ngôn ngữ](/vi/docs/language).

## Đầu người chơi hiện sai texture

Vật phẩm giá treo là đầu người chơi, và skin phía sau nó được tải từ Mojang trong lần đầu client
nhìn thấy. Người chơi lúc đó đang ngoại tuyến sẽ thấy đầu người trơn cho tới khi bộ nhớ đệm của game
cập nhật.

Vào lại một lần khi đang trực tuyến thường là đủ. Nếu vẫn kẹt, hãy thoát game, xóa vài thư mục mới
nhất trong `.minecraft/assets/skins/`, rồi mở lại.
