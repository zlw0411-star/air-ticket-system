# API 契约

统一响应：

```json
{ "code": 0, "message": "ok", "data": {} }
```

业务错误 `code != 0`。未登录返回 401，无权限返回 403。

## Auth

- `POST /api/auth/register` `{username,password,realName,idCard,phone,email}`
- `POST /api/auth/login` `{username,password}` -> `{token,user}`
- `GET /api/auth/me`

## User

- `GET /api/users/profile`
- `PUT /api/users/profile` `{realName,phone,email}`
- `PUT /api/users/password` `{oldPassword,newPassword}`

## Flight

- `GET /api/flights?departureCity=&arrivalCity=&date=&page=1&size=10`
- `GET /api/flights/{id}`
- `GET /api/flights/cities`

## Order

- `POST /api/orders` `{flightId,cabinType,passengers:[{name,idCard,phone}]}`
- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders/{id}/pay`
- `POST /api/orders/{id}/cancel`

## Ticket

- `POST /api/tickets/{id}/refund`
- `POST /api/tickets/{id}/reschedule` `{targetFlightId,cabinType}`

## Admin

- `GET /api/admin/users?keyword=`
- `PUT /api/admin/users/{id}/status` `{enabled}`
- `GET /api/admin/flights`
- `POST /api/admin/flights`
- `PUT /api/admin/flights/{id}`
- `PUT /api/admin/flights/{id}/status` `{status}`
- `GET /api/admin/orders`
- `GET /api/admin/statistics`

角色：`ADMIN`、`PASSENGER`。舱位：`ECONOMY`、`BUSINESS`。
订单状态：`PENDING_PAYMENT`、`PAID`、`CANCELLED`、`REFUNDED`。
机票状态：`PENDING`、`ISSUED`、`RESCHEDULED`、`REFUNDED`、`CANCELLED`。
航班状态：`SCHEDULED`、`CANCELLED`、`DEPARTED`、`ARRIVED`。
