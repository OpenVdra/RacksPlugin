---
title: Plugin bảo vệ đất
---

# Plugin bảo vệ đất

Việc phá giá treo, đổi vật phẩm trên đó hoặc xoay hướng nó đều được kiểm tra qua vùng WorldGuard và
mảnh đất GriefPrevention. Không cần cài thêm gì ngoài chính plugin bảo vệ đó, Racks tự nhận ra nó khi
khởi động.

Đặt giá treo xuống là một lượt đặt block bình thường, nên cả hai plugin đã tự lo phần đó rồi.

## Mỗi hành động cần gì

| Hành động trên giá treo | Cờ WorldGuard | Mức tin cậy GriefPrevention |
|---|---|---|
| Phá | `block-break` | Build |
| Đổi vật phẩm, hoặc xoay giá treo | `interact` | Container |

Ngoài mọi vùng và mảnh đất, mọi hành động đều được cho phép.

## Tắt một lượt kiểm tra

```yaml
protection:
  worldguard: true
  griefprevention: true
```

Hai cái độc lập nhau. `worldguard: false` chỉ bỏ qua phần kiểm tra của WorldGuard,
`griefprevention: false` chỉ bỏ qua phần của GriefPrevention. Mỗi cái đều bị bỏ qua khi plugin tương
ứng chưa cài, và cả hai đều không ảnh hưởng tới việc đặt giá treo xuống.

## WorldGuard

**Tải về:** [Modrinth](https://modrinth.com/plugin/worldguard)

Phần kiểm tra dùng đúng cơ chế build test của WorldGuard, nên **thành viên vùng luôn được phép**. Với
người không phải thành viên, ví dụ trong vùng công cộng hoặc trong `__global__`, cần bật cờ rõ ràng:

```bash
/rg flag <region> block-break allow
/rg flag <region> interact allow
```

Đặt một cờ thành `deny` sẽ chặn cả thành viên:

```bash
/rg flag <region> interact deny
```

### Bỏ qua kiểm tra

Operator và những ai có quyền `worldguard.region.bypass`, hoặc dạng theo từng thế giới
`worldguard.region.bypass.<world>`, đều bỏ qua cả hai lượt kiểm tra. Hãy thử bằng tài khoản người
chơi thường.

## GriefPrevention

**Tải về:** [Modrinth](https://modrinth.com/plugin/griefprevention)

Phá giá treo cần **mức tin cậy Build**, giống như phá một block. Đổi vật phẩm hoặc xoay giá treo chỉ
cần mức nhẹ hơn là **Container**, đúng mức mà một cái rương yêu cầu, vì hai việc đó không phá hủy giá
treo.

```bash
/trust <player>
/containertrust <player>
```

`/trust` bao gồm cả phá, đổi và xoay. `/containertrust` chỉ cho đổi và xoay. `/accesstrust`, vốn dành
cho nút bấm và cửa, không đủ cho bất kỳ hành động nào ở trên.

### Bỏ qua kiểm tra

Chủ mảnh đất, người được tin cậy, mảnh đất admin và chế độ `/ignoreclaims` của từng người chơi đều đã
được tôn trọng sẵn. Không có quyền nào khác cần cấp thêm.
