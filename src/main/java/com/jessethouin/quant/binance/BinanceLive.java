package com.jessethouin.quant.binance;

import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.Objects.requireNonNullElse;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.repos.PortfolioRepository;
import com.jessethouin.quant.binance.subscriptions.BinanceExecutionReportsSubscription;
import com.jessethouin.quant.binance.subscriptions.BinanceKlineSubscription;
import com.jessethouin.quant.binance.subscriptions.BinanceOrderBookSubscription;
import com.jessethouin.quant.binance.subscriptions.BinanceStopLossSubscription;
import com.jessethouin.quant.binance.subscriptions.BinanceTestOrderSubscription;
import com.jessethouin.quant.binance.subscriptions.BinanceTickerSubscription;
import com.jessethouin.quant.binance.subscriptions.BinanceTradeSubscription;
import com.jessethouin.quant.broker.Fundamentals;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Broker;
import io.reactivex.disposables.CompositeDisposable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import lombok.Getter;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.Disposable.Composite;
import reactor.core.Disposables;

@Component
@Transactional
public class BinanceLive {
    private static final CompositeDisposable COMPOSITE_DISPOSABLE = new CompositeDisposable();
    private static final Composite SPRING_COMPOSITE_DISPOSABLE = Disposables.composite();
    @Getter
    private Portfolio portfolio;
    private final List<Fundamentals> fundamentalsList = new ArrayList<>();
    private final PlatformTransactionManager transactionManager;
    private final BinanceTestOrderSubscription binanceTestOrderSubscription;
    private final PortfolioRepository portfolioRepository;
    private TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    public BinanceLive(PortfolioRepository portfolioRepository, PlatformTransactionManager transactionManager, EntityManager entityManager, BinanceTestOrderSubscription binanceTestOrderSubscription) {
        this.portfolioRepository = portfolioRepository;
        this.transactionManager = transactionManager;
        this.entityManager = entityManager;
        this.binanceTestOrderSubscription = binanceTestOrderSubscription;
    }

    public void doLive() {
        long start = new Date().getTime();
        CONFIG.setBacktestStart(new Date(start - Duration.ofHours(CONFIG.getRecalibrateHours()).toMillis()));
        CONFIG.setBacktestEnd(new Date(start));

        transactionTemplate = new TransactionTemplate(transactionManager);

        portfolio = requireNonNullElse(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc(), Util.createPortfolio());
        savePortfolio();
        BinanceUtil.reconcile(portfolio);
        BinanceUtil.showWallets();
        recalibrate();
        BinanceUtil.showTradingFees(portfolio);

        if (CONFIG.getBroker() == Broker.BINANCE_TEST) {
            SPRING_COMPOSITE_DISPOSABLE.add(binanceTestOrderSubscription.subscribe());
        } else {
            COMPOSITE_DISPOSABLE.add(BinanceExecutionReportsSubscription.builder().build().subscribe());
        }

        List<CurrencyPair> currencyPairs = BinanceUtil.getAllCryptoCurrencyPairs(CONFIG);
        currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceStopLossSubscription.builder().fundamentals(getFundamentals(currencyPair)).build().subscribe()));
        switch (CONFIG.getDataFeed()) {
            case KLINE -> currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceKlineSubscription.builder().fundamentals(getFundamentals(currencyPair)).build().subscribe()));
            case TICKER -> currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceTickerSubscription.builder().fundamentals(getFundamentals(currencyPair)).build().subscribe()));
            case TRADE -> currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceTradeSubscription.builder().fundamentals(getFundamentals(currencyPair)).build().subscribe()));
            case ORDER_BOOK -> currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceOrderBookSubscription.builder().fundamentals(getFundamentals(currencyPair)).build().subscribe()));
        }
    }

    private Fundamentals getFundamentals(CurrencyPair currencyPair) {
        Fundamentals fundamentals = new Fundamentals(currencyPair, portfolio);
        fundamentalsList.add(fundamentals);
        return fundamentals;
    }

    @Scheduled(fixedRateString = "#{${recalibrateFreq} * 1000}", initialDelayString = "#{${recalibrateFreq} * 1000}")
    protected void recalibrate() {
        if (CONFIG.isRecalibrate()) Util.relacibrate(CONFIG, true);
    }

    // @Scheduled(fixedRate = 1000, initialDelay = 1000)
    protected void savePortfolio() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NonNull TransactionStatus status) {
                portfolio = entityManager.merge(portfolio);
            }
        });
        // fundamentals are not managed by Spring/JPA but they have elements from the merged portfolio, which is why we have to update them manually
        fundamentalsList.forEach(fundamentals -> fundamentals.update(portfolio));
    }
}