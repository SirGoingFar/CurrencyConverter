package com.sirgoingfar.currencyconverter.models;

import android.app.Application;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.sirgoingfar.currencyconverter.App;
import com.sirgoingfar.currencyconverter.models.data.Currency;
import com.sirgoingfar.currencyconverter.models.data.CurrencyData;
import com.sirgoingfar.currencyconverter.utils.JsonUtil;
import com.sirgoingfar.currencyconverter.utils.Pref;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class CalculatorViewModel extends AndroidViewModel {

    private EventBus eventBus;
    private Pref pref = Pref.getsInstance();
    private MutableLiveData<List<Currency>> currencyListLiveData = new MutableLiveData<>();

    public CalculatorViewModel(@NonNull Application application) {
        super(application);
        eventBus = App.getEventBusInstance();
        init();
    }

    private void init() {
        eventBus.post("prepare_list");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    private void prepareCurrencyList(String text) {

        Toast.makeText(getApplication(), text, Toast.LENGTH_SHORT).show();

        if (pref.isCurrencyListInCache()) {
            postCurrencyList(pref.getCurrencyList());
            return;
        }

        //parse JSON string to List<Currency>
        String currencyDataJson = JsonUtil.getCurrencyDataString(getApplication());
        currencyDataJson = JsonUtil.sanitizeJsonString(currencyDataJson);

        if (!TextUtils.isEmpty(currencyDataJson)) {
            try {

                CurrencyData data = new Gson().fromJson(currencyDataJson, new TypeToken<CurrencyData>() {
                }.getType());

                List<CurrencyData.CountryCurrency> countryCurrencies = data.getCountryCurrencies();

                //Generate the currency objects
                if (countryCurrencies.isEmpty())
                    return;

                ArrayList<Currency> currenciesArrayList = new ArrayList<>();
                for (CurrencyData.CountryCurrency countryCurrency : countryCurrencies) {
                    currenciesArrayList.add(new Currency(countryCurrency.getCurrencyCode(), countryCurrency.getFlag()));
                }

                //cache the resulting list of currencies
                pref.saveCurrencyList(currenciesArrayList);
                postCurrencyList(currenciesArrayList);

            } catch (JsonParseException ex) {
                ex.printStackTrace();
            }
        }
    }

    public LiveData<List<Currency>> getCurrencyListObserver() {
        return currencyListLiveData;
    }

    private void postCurrencyList(List<Currency> data) {
        if (data == null || data.isEmpty())
            return;

        currencyListLiveData.postValue(data);
    }

}
