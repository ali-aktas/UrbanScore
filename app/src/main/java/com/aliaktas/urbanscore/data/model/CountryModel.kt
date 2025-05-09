package com.aliaktas.urbanscore.data.model

data class CountryModel(
    val id: String,       // Firestore'daki ve ISO kod eşleşmesi için
    val name: String,     // Ülkenin İngilizce adı
    val flagUrl: String   // Bayrak URL'i
) {
    companion object {
        // Tüm ülkeleri döndüren metod
        fun getAll(): List<CountryModel> {
            return buildList {
                // Afrika
                add(CountryModel("dz", "Algeria", getFlagUrl("dz")))
                add(CountryModel("ao", "Angola", getFlagUrl("ao")))
                add(CountryModel("bj", "Benin", getFlagUrl("bj")))
                add(CountryModel("bw", "Botswana", getFlagUrl("bw")))
                add(CountryModel("bf", "Burkina Faso", getFlagUrl("bf")))
                add(CountryModel("bi", "Burundi", getFlagUrl("bi")))
                add(CountryModel("cv", "Cape Verde", getFlagUrl("cv")))
                add(CountryModel("cm", "Cameroon", getFlagUrl("cm")))
                add(CountryModel("cf", "Central African Republic", getFlagUrl("cf")))
                add(CountryModel("td", "Chad", getFlagUrl("td")))
                add(CountryModel("km", "Comoros", getFlagUrl("km")))
                add(CountryModel("cg", "Congo", getFlagUrl("cg")))
                add(CountryModel("cd", "DR Congo", getFlagUrl("cd")))
                add(CountryModel("dj", "Djibouti", getFlagUrl("dj")))
                add(CountryModel("eg", "Egypt", getFlagUrl("eg")))
                add(CountryModel("gq", "Equatorial Guinea", getFlagUrl("gq")))
                add(CountryModel("er", "Eritrea", getFlagUrl("er")))
                add(CountryModel("sz", "Eswatini", getFlagUrl("sz"))) // Eski adı Swaziland
                add(CountryModel("et", "Ethiopia", getFlagUrl("et")))
                add(CountryModel("ga", "Gabon", getFlagUrl("ga")))
                add(CountryModel("gm", "Gambia", getFlagUrl("gm")))
                add(CountryModel("gh", "Ghana", getFlagUrl("gh")))
                add(CountryModel("gn", "Guinea", getFlagUrl("gn")))
                add(CountryModel("gw", "Guinea-Bissau", getFlagUrl("gw")))
                add(CountryModel("ci", "Côte d'Ivoire", getFlagUrl("ci")))
                add(CountryModel("ke", "Kenya", getFlagUrl("ke")))
                add(CountryModel("ls", "Lesotho", getFlagUrl("ls")))
                add(CountryModel("lr", "Liberia", getFlagUrl("lr")))
                add(CountryModel("ly", "Libya", getFlagUrl("ly")))
                add(CountryModel("mg", "Madagascar", getFlagUrl("mg")))
                add(CountryModel("mw", "Malawi", getFlagUrl("mw")))
                add(CountryModel("ml", "Mali", getFlagUrl("ml")))
                add(CountryModel("mr", "Mauritania", getFlagUrl("mr")))
                add(CountryModel("mu", "Mauritius", getFlagUrl("mu")))
                add(CountryModel("ma", "Morocco", getFlagUrl("ma")))
                add(CountryModel("mz", "Mozambique", getFlagUrl("mz")))
                add(CountryModel("na", "Namibia", getFlagUrl("na")))
                add(CountryModel("ne", "Niger", getFlagUrl("ne")))
                add(CountryModel("ng", "Nigeria", getFlagUrl("ng")))
                add(CountryModel("rw", "Rwanda", getFlagUrl("rw")))
                add(CountryModel("st", "São Tomé and Príncipe", getFlagUrl("st")))
                add(CountryModel("sn", "Senegal", getFlagUrl("sn")))
                add(CountryModel("sc", "Seychelles", getFlagUrl("sc")))
                add(CountryModel("sl", "Sierra Leone", getFlagUrl("sl")))
                add(CountryModel("so", "Somalia", getFlagUrl("so")))
                add(CountryModel("za", "South Africa", getFlagUrl("za")))
                add(CountryModel("ss", "South Sudan", getFlagUrl("ss")))
                add(CountryModel("sd", "Sudan", getFlagUrl("sd")))
                add(CountryModel("tz", "Tanzania", getFlagUrl("tz")))
                add(CountryModel("tg", "Togo", getFlagUrl("tg")))
                add(CountryModel("tn", "Tunisia", getFlagUrl("tn")))
                add(CountryModel("ug", "Uganda", getFlagUrl("ug")))
                add(CountryModel("zm", "Zambia", getFlagUrl("zm")))
                add(CountryModel("zw", "Zimbabwe", getFlagUrl("zw")))

                // Asya
                add(CountryModel("af", "Afghanistan", getFlagUrl("af")))
                add(CountryModel("am", "Armenia", getFlagUrl("am")))
                add(CountryModel("az", "Azerbaijan", getFlagUrl("az")))
                add(CountryModel("bh", "Bahrain", getFlagUrl("bh")))
                add(CountryModel("bd", "Bangladesh", getFlagUrl("bd")))
                add(CountryModel("bt", "Bhutan", getFlagUrl("bt")))
                add(CountryModel("bn", "Brunei", getFlagUrl("bn")))
                add(CountryModel("kh", "Cambodia", getFlagUrl("kh")))
                add(CountryModel("cn", "China", getFlagUrl("cn")))
                add(CountryModel("cy", "Cyprus", getFlagUrl("cy")))
                add(CountryModel("ge", "Georgia", getFlagUrl("ge")))
                add(CountryModel("in", "India", getFlagUrl("in")))
                add(CountryModel("id", "Indonesia", getFlagUrl("id")))
                add(CountryModel("ir", "Iran", getFlagUrl("ir")))
                add(CountryModel("iq", "Iraq", getFlagUrl("iq")))
                add(CountryModel("il", "Israel", getFlagUrl("il")))
                add(CountryModel("jp", "Japan", getFlagUrl("jp")))
                add(CountryModel("jo", "Jordan", getFlagUrl("jo")))
                add(CountryModel("kz", "Kazakhstan", getFlagUrl("kz")))
                add(CountryModel("kw", "Kuwait", getFlagUrl("kw")))
                add(CountryModel("kg", "Kyrgyzstan", getFlagUrl("kg")))
                add(CountryModel("la", "Laos", getFlagUrl("la")))
                add(CountryModel("lb", "Lebanon", getFlagUrl("lb")))
                add(CountryModel("my", "Malaysia", getFlagUrl("my")))
                add(CountryModel("mv", "Maldives", getFlagUrl("mv")))
                add(CountryModel("mn", "Mongolia", getFlagUrl("mn")))
                add(CountryModel("mm", "Myanmar", getFlagUrl("mm")))
                add(CountryModel("np", "Nepal", getFlagUrl("np")))
                add(CountryModel("kp", "North Korea", getFlagUrl("kp")))
                add(CountryModel("om", "Oman", getFlagUrl("om")))
                add(CountryModel("pk", "Pakistan", getFlagUrl("pk")))
                add(CountryModel("ps", "Palestine", getFlagUrl("ps")))
                add(CountryModel("ph", "Philippines", getFlagUrl("ph")))
                add(CountryModel("qa", "Qatar", getFlagUrl("qa")))
                add(CountryModel("sa", "Saudi Arabia", getFlagUrl("sa")))
                add(CountryModel("sg", "Singapore", getFlagUrl("sg")))
                add(CountryModel("kr", "South Korea", getFlagUrl("kr")))
                add(CountryModel("lk", "Sri Lanka", getFlagUrl("lk")))
                add(CountryModel("sy", "Syria", getFlagUrl("sy")))
                add(CountryModel("tw", "Taiwan", getFlagUrl("tw")))
                add(CountryModel("tj", "Tajikistan", getFlagUrl("tj")))
                add(CountryModel("th", "Thailand", getFlagUrl("th")))
                add(CountryModel("tl", "Timor-Leste", getFlagUrl("tl")))
                add(CountryModel("tr", "Turkiye", getFlagUrl("tr"))) // Güncel adı!
                add(CountryModel("tm", "Turkmenistan", getFlagUrl("tm")))
                add(CountryModel("ae", "United Arab Emirates", getFlagUrl("ae")))
                add(CountryModel("uz", "Uzbekistan", getFlagUrl("uz")))
                add(CountryModel("vn", "Vietnam", getFlagUrl("vn")))
                add(CountryModel("ye", "Yemen", getFlagUrl("ye")))

                // Avrupa
                add(CountryModel("al", "Albania", getFlagUrl("al")))
                add(CountryModel("ad", "Andorra", getFlagUrl("ad")))
                add(CountryModel("at", "Austria", getFlagUrl("at")))
                add(CountryModel("by", "Belarus", getFlagUrl("by")))
                add(CountryModel("be", "Belgium", getFlagUrl("be")))
                add(CountryModel("ba", "Bosnia and Herzegovina", getFlagUrl("ba")))
                add(CountryModel("bg", "Bulgaria", getFlagUrl("bg")))
                add(CountryModel("hr", "Croatia", getFlagUrl("hr")))
                add(CountryModel("cz", "Czech Republic", getFlagUrl("cz")))
                add(CountryModel("dk", "Denmark", getFlagUrl("dk")))
                add(CountryModel("ee", "Estonia", getFlagUrl("ee")))
                add(CountryModel("fi", "Finland", getFlagUrl("fi")))
                add(CountryModel("fr", "France", getFlagUrl("fr")))
                add(CountryModel("de", "Germany", getFlagUrl("de")))
                add(CountryModel("gr", "Greece", getFlagUrl("gr")))
                add(CountryModel("hu", "Hungary", getFlagUrl("hu")))
                add(CountryModel("is", "Iceland", getFlagUrl("is")))
                add(CountryModel("ie", "Ireland", getFlagUrl("ie")))
                add(CountryModel("it", "Italy", getFlagUrl("it")))
                add(CountryModel("xk", "Kosovo", getFlagUrl("xk")))
                add(CountryModel("lv", "Latvia", getFlagUrl("lv")))
                add(CountryModel("li", "Liechtenstein", getFlagUrl("li")))
                add(CountryModel("lt", "Lithuania", getFlagUrl("lt")))
                add(CountryModel("lu", "Luxembourg", getFlagUrl("lu")))
                add(CountryModel("mt", "Malta", getFlagUrl("mt")))
                add(CountryModel("md", "Moldova", getFlagUrl("md")))
                add(CountryModel("mc", "Monaco", getFlagUrl("mc")))
                add(CountryModel("me", "Montenegro", getFlagUrl("me")))
                add(CountryModel("nl", "Netherlands", getFlagUrl("nl")))
                add(CountryModel("mk", "North Macedonia", getFlagUrl("mk"))) // Güncel adı!
                add(CountryModel("no", "Norway", getFlagUrl("no")))
                add(CountryModel("pl", "Poland", getFlagUrl("pl")))
                add(CountryModel("pt", "Portugal", getFlagUrl("pt")))
                add(CountryModel("ro", "Romania", getFlagUrl("ro")))
                add(CountryModel("ru", "Russia", getFlagUrl("ru")))
                add(CountryModel("sm", "San Marino", getFlagUrl("sm")))
                add(CountryModel("rs", "Serbia", getFlagUrl("rs")))
                add(CountryModel("sk", "Slovakia", getFlagUrl("sk")))
                add(CountryModel("si", "Slovenia", getFlagUrl("si")))
                add(CountryModel("es", "Spain", getFlagUrl("es")))
                add(CountryModel("se", "Sweden", getFlagUrl("se")))
                add(CountryModel("ch", "Switzerland", getFlagUrl("ch")))
                add(CountryModel("ua", "Ukraine", getFlagUrl("ua")))
                add(CountryModel("gb", "United Kingdom", getFlagUrl("gb")))
                add(CountryModel("va", "Vatican City", getFlagUrl("va")))

                // Kuzey Amerika
                add(CountryModel("ag", "Antigua and Barbuda", getFlagUrl("ag")))
                add(CountryModel("bs", "Bahamas", getFlagUrl("bs")))
                add(CountryModel("bb", "Barbados", getFlagUrl("bb")))
                add(CountryModel("bz", "Belize", getFlagUrl("bz")))
                add(CountryModel("ca", "Canada", getFlagUrl("ca")))
                add(CountryModel("cr", "Costa Rica", getFlagUrl("cr")))
                add(CountryModel("cu", "Cuba", getFlagUrl("cu")))
                add(CountryModel("dm", "Dominica", getFlagUrl("dm")))
                add(CountryModel("do", "Dominican Republic", getFlagUrl("do")))
                add(CountryModel("sv", "El Salvador", getFlagUrl("sv")))
                add(CountryModel("gd", "Grenada", getFlagUrl("gd")))
                add(CountryModel("gt", "Guatemala", getFlagUrl("gt")))
                add(CountryModel("ht", "Haiti", getFlagUrl("ht")))
                add(CountryModel("hn", "Honduras", getFlagUrl("hn")))
                add(CountryModel("jm", "Jamaica", getFlagUrl("jm")))
                add(CountryModel("mx", "Mexico", getFlagUrl("mx")))
                add(CountryModel("ni", "Nicaragua", getFlagUrl("ni")))
                add(CountryModel("pa", "Panama", getFlagUrl("pa")))
                add(CountryModel("kn", "Saint Kitts and Nevis", getFlagUrl("kn")))
                add(CountryModel("lc", "Saint Lucia", getFlagUrl("lc")))
                add(CountryModel("vc", "Saint Vincent and the Grenadines", getFlagUrl("vc")))
                add(CountryModel("tt", "Trinidad and Tobago", getFlagUrl("tt")))
                add(CountryModel("us", "United States", getFlagUrl("us")))

                // Güney Amerika
                add(CountryModel("ar", "Argentina", getFlagUrl("ar")))
                add(CountryModel("bo", "Bolivia", getFlagUrl("bo")))
                add(CountryModel("br", "Brazil", getFlagUrl("br")))
                add(CountryModel("cl", "Chile", getFlagUrl("cl")))
                add(CountryModel("co", "Colombia", getFlagUrl("co")))
                add(CountryModel("ec", "Ecuador", getFlagUrl("ec")))
                add(CountryModel("gy", "Guyana", getFlagUrl("gy")))
                add(CountryModel("py", "Paraguay", getFlagUrl("py")))
                add(CountryModel("pe", "Peru", getFlagUrl("pe")))
                add(CountryModel("sr", "Suriname", getFlagUrl("sr")))
                add(CountryModel("uy", "Uruguay", getFlagUrl("uy")))
                add(CountryModel("ve", "Venezuela", getFlagUrl("ve")))

                // Okyanusya
                add(CountryModel("au", "Australia", getFlagUrl("au")))
                add(CountryModel("fj", "Fiji", getFlagUrl("fj")))
                add(CountryModel("ki", "Kiribati", getFlagUrl("ki")))
                add(CountryModel("mh", "Marshall Islands", getFlagUrl("mh")))
                add(CountryModel("fm", "Micronesia", getFlagUrl("fm")))
                add(CountryModel("nr", "Nauru", getFlagUrl("nr")))
                add(CountryModel("nz", "New Zealand", getFlagUrl("nz")))
                add(CountryModel("pw", "Palau", getFlagUrl("pw")))
                add(CountryModel("pg", "Papua New Guinea", getFlagUrl("pg")))
                add(CountryModel("ws", "Samoa", getFlagUrl("ws")))
                add(CountryModel("sb", "Solomon Islands", getFlagUrl("sb")))
                add(CountryModel("to", "Tonga", getFlagUrl("to")))
                add(CountryModel("tv", "Tuvalu", getFlagUrl("tv")))
                add(CountryModel("vu", "Vanuatu", getFlagUrl("vu")))

                // Diğer
                add(CountryModel("other", "Other", ""))
            }
        }

        // ISO kodundan bayrak URL'ini oluşturan metod
        private fun getFlagUrl(countryCode: String): String {
            return "https://flagcdn.com/w320/$countryCode.png"
        }
    }
}