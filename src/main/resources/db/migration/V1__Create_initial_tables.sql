CREATE TABLE eod_data (
  id SERIAL PRIMARY KEY,
  symbol CHAR(10) NOT NULL,
  date DATE NOT NULL,
  open DECIMAL(8,3) NOT NULL,
  high DECIMAL(8,3) NOT NULL,
  low DECIMAL(8,3) NOT NULL,
  close DECIMAL(8,3) NOT NULL,
  volume BIGINT NOT NULL,
  adj_close DECIMAL(8,3) NOT NULL
);

CREATE INDEX ON eod_data (symbol);