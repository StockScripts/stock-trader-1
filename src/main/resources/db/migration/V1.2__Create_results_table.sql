CREATE TABLE backtest (
  id SERIAL PRIMARY KEY,
  date TIMESTAMP NOT NULL,
  display_id VARCHAR NOT NULL UNIQUE
);

CREATE TABLE algo_result (
  algo_name VARCHAR NOT NULL,
  ann_returns DECIMAL(8,3) NOT NULL,
  ann_volatility DECIMAL(8,3) NOT NULL,
  max_drawdown DECIMAL(8,3) NOT NULL,
  sharpe DECIMAL(8,3) NOT NULL,
  sortino DECIMAL(8,3) NOT NULL,
  calmar DECIMAL(8,3),
  historical_values DECIMAL(15,2)[] NOT NULL,
  historical_dates DATE[] NOT NULL,
  backtest_id INT NOT NULL references backtest(id) ON DELETE CASCADE
);

CREATE INDEX ON backtest (display_id);
CREATE INDEX ON algo_result (backtest_id);