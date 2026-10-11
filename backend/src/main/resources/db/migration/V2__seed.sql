INSERT INTO flight (flight_no, airline, departure_city, arrival_city, departure_airport, arrival_airport, departure_time, arrival_time, aircraft_type, status)
SELECT t.flight_no, t.airline, t.departure_city, t.arrival_city, t.departure_airport, t.arrival_airport,
       DATE_ADD(CURDATE(), INTERVAL d.day_offset DAY) + INTERVAL t.dep_hour HOUR + INTERVAL t.dep_min MINUTE,
       DATE_ADD(CURDATE(), INTERVAL d.day_offset DAY) + INTERVAL t.arr_hour HOUR + INTERVAL t.arr_min MINUTE,
       t.aircraft_type, 'SCHEDULED'
FROM (
  SELECT 1 AS day_offset UNION ALL SELECT 2 UNION ALL SELECT 3
) d
CROSS JOIN (
  SELECT 'CA1501' AS flight_no, '中国国际航空' AS airline, '北京' AS departure_city, '上海' AS arrival_city, '北京首都T3' AS departure_airport, '上海虹桥T2' AS arrival_airport, 8 AS dep_hour, 0 AS dep_min, 10 AS arr_hour, 10 AS arr_min, 'A320' AS aircraft_type
  UNION ALL SELECT 'MU5101', '中国东方航空', '上海', '北京', '上海虹桥T2', '北京首都T3', 11, 20, 13, 30, 'A321'
  UNION ALL SELECT 'CZ3101', '中国南方航空', '广州', '北京', '广州白云T2', '北京大兴', 9, 10, 12, 5, 'B737'
  UNION ALL SELECT 'CA1301', '中国国际航空', '北京', '广州', '北京首都T3', '广州白云T2', 14, 0, 17, 10, 'A330'
  UNION ALL SELECT 'ZH9801', '深圳航空', '深圳', '上海', '深圳宝安T3', '上海浦东T2', 7, 40, 10, 5, 'A320'
  UNION ALL SELECT 'MU5401', '中国东方航空', '上海', '成都', '上海虹桥T2', '成都双流T2', 13, 15, 16, 20, 'A321'
  UNION ALL SELECT '3U8881', '四川航空', '成都', '北京', '成都天府T2', '北京大兴', 8, 30, 11, 10, 'A320neo'
  UNION ALL SELECT 'HU7601', '海南航空', '北京', '杭州', '北京首都T3', '杭州萧山T1', 16, 40, 18, 50, 'B737'
  UNION ALL SELECT 'MF8101', '厦门航空', '杭州', '深圳', '杭州萧山T1', '深圳宝安T3', 9, 50, 12, 10, 'B737'
  UNION ALL SELECT 'CA1231', '中国国际航空', '西安', '上海', '西安咸阳T3', '上海浦东T2', 10, 20, 12, 40, 'A320'
  UNION ALL SELECT 'MU2351', '中国东方航空', '重庆', '广州', '重庆江北T3', '广州白云T2', 15, 10, 17, 20, 'A321'
  UNION ALL SELECT 'CZ3456', '中国南方航空', '广州', '成都', '广州白云T2', '成都双流T2', 18, 0, 20, 30, 'A320'
) t;

INSERT INTO flight_inventory (flight_id, cabin_type, price, total_seats, available_seats, version)
SELECT f.id, 'ECONOMY',
  CASE
    WHEN f.departure_city IN ('北京','上海') AND f.arrival_city IN ('上海','北京') THEN 680.00
    WHEN f.departure_city IN ('北京','广州') AND f.arrival_city IN ('广州','北京') THEN 980.00
    WHEN f.departure_city IN ('上海','成都') AND f.arrival_city IN ('成都','上海') THEN 890.00
    WHEN f.departure_city IN ('北京','杭州') AND f.arrival_city IN ('杭州','北京') THEN 620.00
    WHEN f.departure_city IN ('深圳','上海') AND f.arrival_city IN ('上海','深圳') THEN 860.00
    WHEN f.departure_city IN ('西安','上海') AND f.arrival_city IN ('上海','西安') THEN 790.00
    WHEN f.departure_city IN ('成都','广州') AND f.arrival_city IN ('广州','成都') THEN 920.00
    WHEN f.departure_city IN ('杭州','深圳') AND f.arrival_city IN ('深圳','杭州') THEN 720.00
    ELSE 760.00
  END,
  120, 120, 0
FROM flight f;

INSERT INTO flight_inventory (flight_id, cabin_type, price, total_seats, available_seats, version)
SELECT f.id, 'BUSINESS',
  CASE
    WHEN f.departure_city IN ('北京','上海') AND f.arrival_city IN ('上海','北京') THEN 1880.00
    WHEN f.departure_city IN ('北京','广州') AND f.arrival_city IN ('广州','北京') THEN 2580.00
    WHEN f.departure_city IN ('上海','成都') AND f.arrival_city IN ('成都','上海') THEN 2380.00
    WHEN f.departure_city IN ('北京','杭州') AND f.arrival_city IN ('杭州','北京') THEN 1680.00
    WHEN f.departure_city IN ('深圳','上海') AND f.arrival_city IN ('上海','深圳') THEN 2280.00
    WHEN f.departure_city IN ('西安','上海') AND f.arrival_city IN ('上海','西安') THEN 2100.00
    WHEN f.departure_city IN ('成都','广州') AND f.arrival_city IN ('广州','成都') THEN 2400.00
    WHEN f.departure_city IN ('杭州','深圳') AND f.arrival_city IN ('深圳','杭州') THEN 1900.00
    ELSE 1980.00
  END,
  16, 16, 0
FROM flight f;
