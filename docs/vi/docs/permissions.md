---
title: Quyền
---

# Quyền

## Dùng giá treo

**`racks.use`** &nbsp; mặc định: **mọi người**
Đặt giá treo, treo đồ lên, đổi tư thế và phá giá treo.

Quyền này bật sẵn cho mọi người vì data pack gốc không giới hạn gì cả. Một bản cài mới hoạt động
đúng như data pack.

Để chặn hoàn toàn một nhóm dùng giá treo, hãy phủ định quyền này cho nhóm đó trong plugin phân quyền
của bạn. Người chơi không có quyền vẫn chế tạo được vật phẩm, nhưng khi đặt xuống thì giá treo quay
lại túi đồ, và bấm vào giá treo có sẵn cũng không có tác dụng.

::: tip Bảo vệ công trình
`racks.use` chỉ có bật hoặc tắt. Để chặn người chơi phá giá treo của nhau ở một số khu vực, hãy dùng
WorldGuard hoặc GriefPrevention. Racks kiểm tra cả hai trước khi một giá treo bị phá hoặc bị đổi vật
phẩm, xem [Plugin bảo vệ đất](/vi/docs/protections).
:::

## Lệnh

Mỗi lệnh có quyền riêng. Không có quyền cha dùng chung, nên cấp quyền này không đồng nghĩa cấp luôn
quyền kia. Cả hai mặc định dành cho người vận hành.

**`racks.command.give`** - `/racks give`: phát giá treo thuộc loại gỗ bất kỳ.

**`racks.command.reload`** - `/racks reload`: đọc lại cấu hình và file ngôn ngữ.

Người chơi không có cả hai quyền trên sẽ không nhìn thấy `/racks` trong danh sách lệnh.
