package com.example.data.model

enum class IndonesianMonth(
    val number: Int,
    val displayName: String,
    val shortName: String,
    val folderName: String
) {
    JANUARI(1, "Januari", "Jan", "01_januari"),
    FEBRUARI(2, "Februari", "Feb", "02_februari"),
    MARET(3, "Maret", "Mar", "03_maret"),
    APRIL(4, "April", "Apr", "04_april"),
    MEI(5, "Mei", "Mei", "05_mei"),
    JUNI(6, "Juni", "Jun", "06_juni"),
    JULI(7, "Juli", "Jul", "07_juli"),
    AGUSTUS(8, "Agustus", "Agu", "08_agustus"),
    SEPTEMBER(9, "September", "Sep", "09_september"),
    OKTOBER(10, "Oktober", "Okt", "10_oktober"),
    NOVEMBER(11, "November", "Nov", "11_november"),
    DESEMBER(12, "Desember", "Des", "12_desember");

    val formattedNumber: String
        get() = number.toString().padStart(2, '0')

    companion object {
        fun fromNumber(monthNumber: Int): IndonesianMonth {
            return entries.firstOrNull { it.number == monthNumber } ?: JANUARI
        }
    }
}
