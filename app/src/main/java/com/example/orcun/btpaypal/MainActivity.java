package com.example.orcun.btpaypal;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.internal.HttpClient;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import java.util.HashMap;
import java.util.Map;



public class MainActivity extends AppCompatActivity implements PaymentMethodNonceCreatedListener {

    final String get_token = "http://orcodevbox.co.uk/BTOrcun/tokenGen.php";
    final String send_payment_details = "http://orcodevbox.co.uk/BTOrcun/iosPayment.php";
    String token, amount, nonce;
    Button btnPay;
    ProgressBar spinner;
    TextView tokenTextView;
    BraintreeFragment mBraintreeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Invoke HTTP request to get a client-token
        new HttpRequest().execute();

        // Handle UI elements
        tokenTextView = (TextView) findViewById(R.id.textView);
        spinner = (ProgressBar) findViewById(R.id.progressBar);
        btnPay = (Button) findViewById(R.id.btnPay);
        btnPay.setClickable(false);

        // Button listener. Invoke onBraintreeSubmit() function to launch PayPal flow
        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spinner.setVisibility(View.VISIBLE);
                onBraintreeSubmit();
            }
        });
    }

    // Initialize PayPal fragment when a client-token is successfully retrieved
    protected void onAuthorizationFetched() {
        tokenTextView.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.INVISIBLE);
        btnPay.setClickable(true);

        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, token);
        } catch (InvalidArgumentException e) {
            //onError(e);
        }
    }

    // Launch PayPal flow
    public void onBraintreeSubmit() {

        PayPalRequest request = new PayPalRequest("10.00")
                .currencyCode("USD")
                .userAction("commit")
                .intent(PayPalRequest.INTENT_SALE);
        PayPal.requestOneTimePayment(mBraintreeFragment, request);

    }

    //Handle Paypal response here
    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {

        PayPalAccountNonce paypalAccountNonce = (PayPalAccountNonce) paymentMethodNonce;

        nonce=paypalAccountNonce.getNonce();
        amount="10.00";
        Log.d("nonce received", nonce);
        // Access additional information
        /*
        PostalAddress billingAddress = paypalAccountNonce.getBillingAddress();
        String streetAddress = billingAddress.getStreetAddress();
        String extendedAddress = billingAddress.getExtendedAddress();
        String locality = billingAddress.getLocality();
        String countryCodeAlpha2 = billingAddress.getCountryCodeAlpha2();
        String postalCode = billingAddress.getPostalCode();
        String region = billingAddress.getRegion();
        String email = paypalAccountNonce1.getEmail();
        String firstName = paypalAccountNonce1.getFirstName();
        String lastName = paypalAccountNonce1.getLastName();
        String phone = paypalAccountNonce1.getPhone();
        */

        // Send the nonce
        sendPaymentDetails();

    }

    // Function for sending the nonce to server
    private void sendPaymentDetails() {
        tokenTextView.setText("Sending nonce to server...");
        tokenTextView.setVisibility(View.VISIBLE);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        // Request a string response from the provided URL.

        StringRequest stringRequest = new StringRequest(Request.Method.POST, send_payment_details,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        spinner.setVisibility(View.INVISIBLE);
                        tokenTextView.setVisibility(View.INVISIBLE);
                        if(response.contains("Successful"))
                        {
                            Toast.makeText(MainActivity.this, "Transaction successful", Toast.LENGTH_LONG).show();
                            Log.d("Success", "Final Response: " + response.toString());
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Transaction failed", Toast.LENGTH_LONG).show();
                            Log.d("Fail", "Final Response: " + response.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("mylog", "Volley error : " + error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {

                // Add nonce and amount to HTTP request to create a transaction on the server-side
                Map<String, String> params = new HashMap<String, String>();
                params.put("payment_method_nonce", nonce);
                params.put("amount", amount);

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };

        // This prevents multiple HTTP requests
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(stringRequest);
    }

    // HttpRequest class to get a client-token
    private class HttpRequest extends AsyncTask {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected Object doInBackground(Object[] objects) {
            HttpClient client = new HttpClient();
            client.get(get_token, new HttpResponseCallback() {
                @Override
                public void success(String responseBody) {
                    Log.d("mylog", responseBody);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Successfully got a client-token", Toast.LENGTH_SHORT).show();
                        }
                    });
                    token = responseBody;
                    onAuthorizationFetched();
                }

                @Override
                public void failure(Exception exception) {
                    final Exception ex = exception;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Failed to get a client-token: " + ex.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

        }
    }
}
