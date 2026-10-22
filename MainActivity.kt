package com.example.cloneproxymanager

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.net.HttpURLConnection
import java.util.UUID
import java.util.concurrent.Executors

data class ProxyProfile(
    val id: String,
    var name: String,
    var host: String,
    var port: Int
)

data class CloneProfile(
    val id: String,
    var name: String,
    var proxyId: String? = null
)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("clone_proxy_manager", MODE_PRIVATE) }
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private var proxies = mutableListOf<ProxyProfile>()
    private var profiles = mutableListOf<CloneProfile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData()
        buildUi()
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "Clone Proxy Manager"
            textSize = 24f
            setTextColor(0xFF172033.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Bản thử nghiệm: quản lý hồ sơ và kiểm tra Proxy HTTP/HTTPS. Chưa định tuyến lưu lượng của ứng dụng nhân bản."
            textSize = 14f
            setPadding(0, dp(8), 0, dp(12))
        })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "Thêm hồ sơ"
            setOnClickListener { showAddProfileDialog() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(Button(this).apply {
            text = "Thêm Proxy"
            setOnClickListener { showAddProxyDialog() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions)

        root.addView(sectionTitle("HỒ SƠ NHÂN BẢN"))
        root.addView(TextView(this).apply {
            text = "Mỗi hồ sơ có thể được gán một cấu hình Proxy."
            textSize = 13f
        })
        renderProfiles()

        root.addView(sectionTitle("DANH SÁCH PROXY"))
        renderProxies()
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 16f
        setTextColor(0xFF2355A6.toInt())
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun renderProfiles() {
        root.removeViewsIfNeeded("profiles_container")
        val container = LinearLayout(this).apply {
            tag = "profiles_container"
            orientation = LinearLayout.VERTICAL
        }
        if (profiles.isEmpty()) {
            container.addView(emptyText("Chưa có hồ sơ. Nhấn “Thêm hồ sơ”."))
        }
        profiles.forEach { profile ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(0xFFF1F5FA.toInt())
            }
            card.addView(TextView(this).apply {
                text = profile.name
                textSize = 17f
                setTextColor(0xFF172033.toInt())
            })
            val names = mutableListOf("Không dùng Proxy")
            names.addAll(proxies.map { "${it.name} (${it.host}:${it.port})" })
            val spinner = Spinner(this)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
            val selected = proxies.indexOfFirst { it.id == profile.proxyId }
            spinner.setSelection(if (selected >= 0) selected + 1 else 0)
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    profile.proxyId = if (position == 0) null else proxies.getOrNull(position - 1)?.id
                    saveData()
                }
            }
            card.addView(spinner)

            val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            buttons.addView(Button(this@MainActivity).apply {
                text = "Kiểm tra Proxy"
                setOnClickListener {
                    val proxy = proxies.firstOrNull { it.id == profile.proxyId }
                    if (proxy == null) {
                        toast("Hãy gán Proxy cho hồ sơ trước.")
                    } else {
                        testProxy(proxy)
                    }
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            buttons.addView(Button(this@MainActivity).apply {
                text = "Xóa hồ sơ"
                setOnClickListener {
                    profiles.removeAll { it.id == profile.id }
                    saveData()
                    refreshUi()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(buttons)
            container.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            })
        }
        root.addView(container)
    }

    private fun renderProxies() {
        root.removeViewsIfNeeded("proxies_container")
        val container = LinearLayout(this).apply {
            tag = "proxies_container"
            orientation = LinearLayout.VERTICAL
        }
        if (proxies.isEmpty()) container.addView(emptyText("Chưa có Proxy nào được lưu."))
        proxies.forEach { proxy ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(0xFFF8F9FB.toInt())
            }
            row.addView(TextView(this).apply {
                text = "${proxy.name}\n${proxy.host}:${proxy.port}"
                textSize = 15f
            })
            row.addView(Button(this).apply {
                text = "Kiểm tra kết nối"
                setOnClickListener { testProxy(proxy) }
            })
            row.addView(Button(this).apply {
                text = "Xóa Proxy"
                setOnClickListener {
                    proxies.removeAll { it.id == proxy.id }
                    profiles.filter { it.proxyId == proxy.id }.forEach { it.proxyId = null }
                    saveData()
                    refreshUi()
                }
            })
            container.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(8)
            })
        }
        root.addView(container)
    }

    private fun LinearLayout.removeViewsIfNeeded(tagName: String) {
        for (i in childCount - 1 downTo 0) {
            if (getChildAt(i).tag == tagName) removeViewAt(i)
        }
    }

    private fun emptyText(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun showAddProfileDialog() {
        val input = EditText(this).apply { hint = "Tên hồ sơ, ví dụ: Tài khoản A" }
        AlertDialog.Builder(this)
            .setTitle("Thêm hồ sơ")
            .setView(input)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) toast("Tên hồ sơ không được để trống.")
                else {
                    profiles.add(CloneProfile(UUID.randomUUID().toString(), name))
                    saveData()
                    refreshUi()
                }
            }.show()
    }

    private fun showAddProxyDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val name = EditText(this).apply { hint = "Tên Proxy" }
        val host = EditText(this).apply { hint = "Host / IP"; singleLine = true }
        val port = EditText(this).apply {
            hint = "Port (ví dụ 8080)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            singleLine = true
        }
        form.addView(name)
        form.addView(host)
        form.addView(port)
        AlertDialog.Builder(this)
            .setTitle("Thêm Proxy HTTP/HTTPS")
            .setView(form)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu") { _, _ ->
                val n = name.text.toString().trim()
                val h = host.text.toString().trim()
                val p = port.text.toString().toIntOrNull()
                if (n.isBlank() || h.isBlank() || p == null || p !in 1..65535) {
                    toast("Vui lòng nhập tên, host và port hợp lệ.")
                } else {
                    proxies.add(ProxyProfile(UUID.randomUUID().toString(), n, h, p))
                    saveData()
                    refreshUi()
                }
            }.show()
    }

    private fun testProxy(proxy: ProxyProfile) {
        toast("Đang kiểm tra Proxy…")
        io.execute {
            val result = try {
                val javaProxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port))
                val connection = (URL("https://api.ipify.org").openConnection(javaProxy) as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                }
                try {
                    val code = connection.responseCode
                    if (code in 200..299) {
                        val ip = connection.inputStream.bufferedReader().use { reader -> reader.readText().trim() }
                        "Proxy hoạt động. IP thoát: $ip"
                    } else {
                        "Proxy phản hồi HTTP $code"
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                "Kiểm tra thất bại: ${e.javaClass.simpleName}: ${e.message ?: "không rõ lỗi"}"
            }
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Kết quả kiểm tra")
                    .setMessage(result)
                    .setPositiveButton("Đóng", null)
                    .show()
            }
        }
    }

    private fun refreshUi() {
        // Rebuild the screen to refresh profile and proxy lists.
        buildUi()
    }

    private fun saveData() {
        val pArr = JSONArray()
        proxies.forEach {
            pArr.put(JSONObject().put("id", it.id).put("name", it.name).put("host", it.host).put("port", it.port))
        }
        val cArr = JSONArray()
        profiles.forEach {
            cArr.put(JSONObject().put("id", it.id).put("name", it.name).put("proxyId", it.proxyId ?: JSONObject.NULL))
        }
        prefs.edit().putString("proxies", pArr.toString()).putString("profiles", cArr.toString()).apply()
    }

    private fun loadData() {
        try {
            val pArr = JSONArray(prefs.getString("proxies", "[]"))
            for (i in 0 until pArr.length()) {
                val o = pArr.getJSONObject(i)
                proxies.add(ProxyProfile(o.getString("id"), o.getString("name"), o.getString("host"), o.getInt("port")))
            }
            val cArr = JSONArray(prefs.getString("profiles", "[]"))
            for (i in 0 until cArr.length()) {
                val o = cArr.getJSONObject(i)
                profiles.add(CloneProfile(
                    o.getString("id"),
                    o.getString("name"),
                    if (o.isNull("proxyId")) null else o.getString("proxyId")
                ))
            }
        } catch (_: Exception) {
            proxies.clear()
            profiles.clear()
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
