# Quant Trading Platform

A Java-based quantitative trading platform that supports algorithmic trading strategies, backtesting, and live trading with multiple brokers.

## Overview

This project is a Spring Boot application designed for quantitative trading across different markets:
- Traditional markets via Alpaca API
- Cryptocurrency markets via Binance

The platform provides tools for developing, testing, and deploying trading strategies with support for both backtesting and live trading environments.

## Features

- **Multiple Market Support**: Trade on traditional markets (Alpaca) and cryptocurrency exchanges (Binance)
- **Backtesting Engine**: Test trading strategies against historical data
  - Parameter optimization to find the best combinations for your strategies
  - Static parameter backtesting for strategy validation
- **Live Trading**: Execute strategies in real-time on supported exchanges
- **Historical Data Capture**: Collect and store market data for future analysis
- **Spring Boot Integration**: Leverages Spring Boot for dependency injection, web services, and data persistence

## Requirements

- Java 23 (with preview features enabled)
- MySQL database
- Maven for dependency management
- API keys for the exchanges you plan to use (Alpaca, Binance, etc.)

## Setup

1. Clone the repository
2. Configure your database connection in `application.properties` or `application.yml`
3. Set up your API keys and other configuration in `quant.properties`
4. Build the project:
   ```
   mvn clean install
   ```

## Usage

The application can be run in different modes by providing command-line arguments:

```
java -jar quant-1.0-SNAPSHOT.jar [mode] [additional arguments]
```

Available modes:

- `combos` - Run backtesting with parameter combinations to find optimal trading strategies
- `backtest` - Run backtesting with static parameters
- `alpaca` - Run live trading using Alpaca API
- `binance` - Run live trading using Binance API
- `capture` - Capture historical data from Binance

Examples:

```
# Run backtesting with parameter combinations
java -jar quant-1.0-SNAPSHOT.jar combos [strategy parameters]

# Run backtesting with static parameters
java -jar quant-1.0-SNAPSHOT.jar backtest

# Run live trading on Alpaca
java -jar quant-1.0-SNAPSHOT.jar alpaca

# Run live trading on Binance
java -jar quant-1.0-SNAPSHOT.jar binance

# Capture historical data from Binance
java -jar quant-1.0-SNAPSHOT.jar capture
```

## Dependencies

- Spring Boot (data-jpa, web, webflux)
- Alpaca Java API
- XChange libraries for cryptocurrency exchanges
- Log4j for logging
- MySQL connector
- Lombok for reducing boilerplate code
- Gson for JSON processing
- Guava for utility functions

## Project Structure

The project is organized into several packages:
- `com.jessethouin.quant.alpaca` - Alpaca API integration
- `com.jessethouin.quant.binance` - Binance API integration
- `com.jessethouin.quant.backtest` - Backtesting functionality
- `com.jessethouin.quant.broker` - Common broker interfaces
- `com.jessethouin.quant.calculators` - Trading strategy calculations
- `com.jessethouin.quant.common` - Shared utilities
- `com.jessethouin.quant.conf` - Configuration classes
- `com.jessethouin.quant.db` - Database operations

## License

### Non-Commercial Use
This project is licensed under Creative Commons Attribution-NonCommercial 4.0 
for educational, research, and personal use.

### Commercial Use
Commercial use requires a separate license. Contact [your-email] for 
commercial licensing terms.

### What This Means:
- ✅ Students can learn from the code
- ✅ Researchers can use it for academic studies  
- ✅ Individual traders can use it for personal trading
- ✅ You can modify and improve the algorithms
- ❌ Trading firms cannot use it commercially without permission
- ❌ Cannot be included in commercial trading products

## Contributing

We welcome contributions to improve this quantitative trading platform! 

### Contribution Types

**✅ Always Welcome (No CLA Required):**
- Bug reports and issue documentation
- Documentation improvements
- Test cases and examples
- Performance benchmarks
- Educational content and tutorials

**📝 Requires Simple CLA:**
- New trading strategies or algorithms
- Core system modifications
- New broker integrations
- Database schema changes

### Contributor License Agreement

For code contributions, we use a simple CLA:

**What you grant us:**
- Right to use your contribution in both open-source and commercial versions
- Right to modify and distribute your contribution

**What you keep:**
- Copyright ownership of your original contribution
- Right to use your contribution in other projects
- Attribution credit in the project

### How to Contribute

1. **Fork and Create Branch**
   ```bash
   git checkout -b feature/new-trading-strategy
   ```

2. **Small Contributions (No CLA needed):**
   - Documentation fixes
   - Bug fixes under 10 lines
   - Test improvements

3. **Larger Contributions (CLA required):**
   - Sign our simple CLA by following the instructions in [CLA.md](CLA.md)
   - Develop your feature
   - Submit pull request

### Code Review Process

1. **Automated Checks:** Tests, linting, security scans
2. **Technical Review:** Code quality, performance, architecture
3. **Trading Logic Review:** Strategy validation, risk assessment
4. **Final Approval:** Maintainer approval for merge

### Contribution Guidelines

**Code Quality:**
- Follow existing code style
- Include unit tests
- Update documentation
- No breaking changes without discussion

**Trading Strategies:**
- Include backtesting results
- Document strategy logic
- Consider risk management implications
- Provide configuration examples

### Trading Strategy Contributions

⚠️ **Important:** All trading strategies must include:
- Backtesting results over multiple market conditions
- Risk assessment and maximum drawdown analysis
- Clear documentation of strategy logic
- Appropriate disclaimers about performance

We reserve the right to reject strategies that:
- Haven't been adequately tested
- Present excessive risk
- Lack proper documentation

### Security Requirements

- Never include real API keys or credentials
- Use placeholder data in examples
- All financial data must be anonymized
- Security review required for broker integrations

## Disclaimer

This software is for educational and research purposes only. Use at your own risk. The authors are not responsible for any financial losses incurred through the use of this software.
