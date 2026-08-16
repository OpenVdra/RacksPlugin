---
title: Plugin bảo vệ đất
---

# Plugin bảo vệ đất

Việc phá giá treo, đổi vật phẩm trên đó hoặc xoay hướng nó đều được kiểm tra qua vùng WorldGuard và
mảnh đất GriefPrevention, dùng cái nào (hoặc cả hai) đang được cài. Không cần cài thêm gì ngoài chính
plugin bảo vệ đó — Racks tự nhận diện khi khởi động.

**Đặt** giá treo thì không cần tích hợp gì cả: đó là một `BlockPlaceEvent` bình thường, giống hệt sự
kiện khi đặt một cái rương hay cây đuốc, nên cả hai plugin đã tự bảo vệ việc đó rồi, Racks không cần
viết thêm code cho phần này.

## Tham khảo nhanh

| Hành động trên giá treo | Cờ (flag) WorldGuard | Mức tin cậy GriefPrevention |
|---|---|---|
| Phá | `block-break` | Build |
| Đổi vật phẩm, hoặc xoay giá treo (cúi + click phải) | `interact` | Inventory (`/containertrust`) |
| Đặt | *(đặt block bình thường — luôn được bảo vệ sẵn)* | *(đặt block bình thường — luôn được bảo vệ sẵn)* |

Ngoài mọi vùng/mảnh đất, mọi hành động đều được cho phép.

## Tắt tính năng này

```yaml
protection:
  worldguard: true
  griefprevention: true
```

Hai cái độc lập nhau: đặt `worldguard: false` để chỉ tắt kiểm tra của WorldGuard, hoặc
`griefprevention: false` để chỉ tắt kiểm tra của GriefPrevention, không ảnh hưởng đến cái còn lại.
Mỗi cái không có tác dụng nếu plugin tương ứng chưa cài, và cả hai đều không ảnh hưởng đến việc đặt
giá treo, vì phần đó luôn được bảo vệ sẵn bất kể hai giá trị này.

## WorldGuard

**Tải về:** [Modrinth](https://modrinth.com/plugin/worldguard)

Kiểm tra dùng đúng cơ chế "build test" của WorldGuard, nên **thành viên vùng luôn được phép**. Với
người không phải thành viên — ví dụ một vùng công cộng hoặc vùng `__global__` — cần bật cờ rõ ràng:

```bash
/rg flag <region> block-break allow
/rg flag <region> interact allow
```

Đặt một cờ thành `deny` sẽ chặn luôn cả thành viên:

```bash
/rg flag <region> interact deny
```

### Bỏ qua kiểm tra (bypass)

Operator và người chơi có quyền `worldguard.region.bypass` (hoặc dạng theo từng thế giới
`worldguard.region.bypass.<world>`) bỏ qua cả hai lượt kiểm tra. Hãy test bằng tài khoản người chơi
thường, không phải op.

## GriefPrevention

**Tải về:** [Modrinth](https://modrinth.com/plugin/griefprevention)

Phá giá treo cần **mức tin cậy Build**, giống như phá một block. Đổi vật phẩm hoặc xoay giá treo chỉ
cần mức nhẹ hơn — **Inventory (Container) trust**, giống mức cần để mở rương — vì hai hành động đó
không phá hủy giá treo.

```bash
/trust <player>          # Build trust: bao gồm cả phá, đổi và xoay
/containertrust <player> # Inventory trust: chỉ đổi và xoay, không phá được
```

`/accesstrust` (chỉ nút bấm và cửa) **không đủ** cho bất kỳ hành động nào ở trên.

### Bỏ qua kiểm tra (bypass)

Chủ mảnh đất, người được tin cậy rõ ràng, mảnh đất admin và chế độ `/ignoreclaims` của từng người chơi
đều được tôn trọng tự động — không có quyền riêng nào khác cần cấp thêm.
