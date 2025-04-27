package com.example.mylib


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlin.concurrent.thread

class BookAdapter(
    private var books: MutableList<Book>,
    private val isSearchMode: Boolean,
    private val isMainMode: Boolean,
    private val context: Context

) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.bookTitle)
        val authorsTextView: TextView = view.findViewById(R.id.bookAuthors)
        val thumbnailImageView: ImageView = view.findViewById(R.id.bookThumbnail)
        val publisherTextView: TextView = view.findViewById(R.id.bookPublisher)
        val publishedDateTextView: TextView = view.findViewById(R.id.bookPublishedDate)
        val pageCountTextView: TextView = view.findViewById(R.id.bookPageCount)
        val moreOptionsButton: ImageButton = view.findViewById(R.id.moreOptionsButton)
        val addBookButton: Button = view.findViewById(R.id.addBookButton)
        val bookReadStatus: TextView = itemView.findViewById(R.id.bookReadStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {



        val book = books[position]
        book.updateReadStatus()
        holder.titleTextView.text = book.title
        holder.authorsTextView.text = book.authors
        holder.publisherTextView.text = "Издательство: ${book.publisher}"
        holder.publishedDateTextView.text = "Дата издания: ${book.publishedDate}"
        holder.pageCountTextView.text = "Страниц: ${book.pageCount}"


        holder.itemView.setOnClickListener {
            showBookDetails(context, book)
        }



        // Используем `isRead` для статуса прочитанности
        holder.bookReadStatus.text = if (book.isRead) "Прочитано" else "Не прочитано"
        holder.bookReadStatus.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (book.isRead) R.color.colorGreenPrimary else R.color.colorRedPrimary
            )
        )

        Log.d("BookAdapter", "Книга: ${book.title}, endDate: ${book.endDate}, isRead: ${book.isRead}")


        // Загружаем изображение книги через Glide
        Glide.with(holder.thumbnailImageView.context)
            .load(book.thumbnail.replace("http://", "https://"))
            .placeholder(R.drawable.book_placeholder)
            .error(R.drawable.book_placeholder)
            .into(holder.thumbnailImageView)

        holder.addBookButton.setOnClickListener {
            val context = holder.itemView.context
            if (saveBookToPreferences(context, book)) {
                Toast.makeText(context, "Книга добавлена в избранное!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Эта книга уже добавлена!", Toast.LENGTH_SHORT).show()
            }
        }
        // В адаптере для каждого элемента мы добавляем обработку состояния чекбокса

        Log.d(
            "BookAdapter",
            "WebReaderLink: ${book.webReaderLink}, PreviewLink: ${book.previewLink}"
        )


        if (isSearchMode) {
            holder.moreOptionsButton.visibility = View.GONE
        }
        if (isMainMode) {
            holder.addBookButton.visibility = View.GONE
        }
        holder.moreOptionsButton.setOnClickListener {
            val context = holder.itemView.context
            val popupMenu = PopupMenu(context, it)
            popupMenu.inflate(R.menu.book_options_menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_add_note -> {
                        val intent = Intent(context, NotesActivity::class.java)
                        intent.putExtra("BOOK_TITLE", book.title)
                        intent.putExtra("IS_READ", book.isRead)
                        context.startActivity(intent)  // Добавляем запуск активности
                        true
                    }


                    R.id.menu_delete -> {
                        removeBook(position, context)
                        true
                    }

                    R.id.menu_favorite -> {
                        pinBookToTop(position)
                        true
                    }

                    R.id.menu_read_on_google_books -> {
                        fetchBookLink(context, book.title) { webReaderLink ->
                            if (!webReaderLink.isNullOrEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webReaderLink))
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Ссылка для чтения отсутствует",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        true
                    }


                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    override fun getItemCount() = books.size

    fun clearBooks() {
        books.clear()
        notifyDataSetChanged()
    }











    fun updateBooks(newBooks: MutableList<Book>) {
        val sharedPreferences = context.getSharedPreferences("book_notes", Context.MODE_PRIVATE)

        newBooks.forEach { book ->
            val savedEndDate = sharedPreferences.getString("${book.title}_end_date_parsed", null)
            if (!savedEndDate.isNullOrEmpty()) {
                book.endDate = LocalDate.parse(savedEndDate)
            }
            book.updateReadStatus()
        }

        books = newBooks
        notifyDataSetChanged()
    }

    fun addBooks(newBooks: List<Book>) {
        val sharedPreferences = context.getSharedPreferences("book_notes", Context.MODE_PRIVATE)

        newBooks.forEach { book ->
            val savedEndDate = sharedPreferences.getString("${book.title}_end_date_parsed", null)
            if (!savedEndDate.isNullOrEmpty()) {
                book.endDate = LocalDate.parse(savedEndDate)
            }
            book.updateReadStatus()
        }

        val startPosition = books.size
        books.addAll(newBooks)
        notifyItemRangeInserted(startPosition, newBooks.size)
    }


    fun removeBook(position: Int, context: Context) {
        val book = books[position]
        books.removeAt(position)
        notifyItemRemoved(position)
        removeBookFromPreferences(context, book)

        decrementBookCount(context)
    }

    fun pinBookToTop(position: Int) {
        val book = books[position]
        book.isFavorite = true  // Отмечаем книгу как избранную

        books.removeAt(position)
        books.add(0, book)  // Перемещаем в начало

        books.sortByDescending { it.isFavorite } // Убедимся, что избранные сверху

        notifyDataSetChanged()
        saveBookListToPreferences()
    }


    private fun saveBookListToPreferences() {
        val sharedPreferences = context.getSharedPreferences("saved_books", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        editor.putString("books", gson.toJson(books))
        editor.apply()
    }


    fun updateBookStatus(bookTitle: String, isRead: Boolean) {
        // Ищем книгу по её названию
        val book = books.find { it.title == bookTitle }
        book?.let {
            it.isRead = isRead  // Обновляем статус
            notifyDataSetChanged()  // Уведомляем адаптер, что данные изменились
        }
    }


    private fun showBookDetails(context: Context, book: Book) {
        val message = """
        Название: ${book.title}
        Автор: ${book.authors}
        Издательство: ${book.publisher}
        Дата издания: ${book.publishedDate}
        Страниц: ${book.pageCount}
        Описание: ${book.description}
    """.trimIndent()

        AlertDialog.Builder(context)
            .setTitle("Информация о книге")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // Метод для сохранения книги в SharedPreferences
    private fun saveBookToPreferences(context: Context, book: Book): Boolean {
        val sharedPreferences = context.getSharedPreferences("saved_books", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        // Устанавливаем флаг isPinned как true (книга закреплена)
        book.isFavorite = true

        // Читаем текущий список книг из SharedPreferences
        val json = sharedPreferences.getString("books", "[]")
        val type = object : TypeToken<MutableList<Book>>() {}.type
        val bookList: MutableList<Book> = gson.fromJson(json, type) ?: mutableListOf()

        // Проверяем, есть ли книга в списке
        if (bookList.any { it.title == book.title }) return false

        // Добавляем книгу в список
        bookList.add(book)

        // Сортируем книги так, чтобы закрепленные были сверху
        bookList.sortByDescending { it.isFavorite }

        // Сохраняем обновленный список в SharedPreferences
        editor.putString("books", gson.toJson(bookList))
        editor.apply()

        // Увеличиваем счетчик книг
        incrementBookCount(context)

        return true
    }






    // Изменение в методе на удаление книги
    private fun removeBookFromPreferences(context: Context, book: Book) {
        val sharedPreferences = context.getSharedPreferences("saved_books", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = sharedPreferences.getString("books", "[]")
        val type = object : TypeToken<MutableList<Book>>() {}.type
        val bookList: MutableList<Book> = gson.fromJson(json, type) ?: mutableListOf()

        // Если книга закреплена, просто снимаем флаг
        bookList.find { it.title == book.title }?.let { it.isFavorite = false }

        // Фильтруем список (если книга не закреплена, удаляем)
        val updatedBookList = bookList.filter { it.isFavorite || it.title != book.title }

        // Сохраняем обновленный список
        editor.putString("books", gson.toJson(updatedBookList))
        editor.apply()

        // Уменьшаем счетчик книг
        decrementBookCount(context)
    }




    // Метод для увеличения счетчика книг
    private fun incrementBookCount(context: Context) {
        val sharedPreferences = context.getSharedPreferences("book_count", Context.MODE_PRIVATE)
        val currentCount = sharedPreferences.getInt("count", 0)
        val editor = sharedPreferences.edit()
        editor.putInt("count", currentCount + 1)
        editor.apply()
    }

    // Метод для уменьшения счетчика книг
    private fun decrementBookCount(context: Context) {
        val sharedPreferences = context.getSharedPreferences("book_count", Context.MODE_PRIVATE)
        val currentCount = sharedPreferences.getInt("count", 0)
        val editor = sharedPreferences.edit()
        if (currentCount > 0) {
            editor.putInt("count", currentCount - 1)
        }
        editor.apply()
    }

    private fun fetchBookLink(context: Context, title: String, callback: (String?) -> Unit) {
        thread {
            try {
                val apiKey = "AIzaSyDjkmHuXtJRPtNC6ZDDl_V1FoOXJVkSNy8"
                val query = title.replace(" ", "+")
                val searchUrl = "https://www.googleapis.com/books/v1/volumes?q=$query&key=$apiKey"

                val url = URL(searchUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.use { it.readText() }

                val jsonObject = JSONObject(responseText)
                val items = jsonObject.optJSONArray("items")

                var webReaderLink: String? = null
                if (items != null && items.length() > 0) {
                    val firstBook = items.getJSONObject(0)
                    val accessInfo = firstBook.optJSONObject("accessInfo")
                    webReaderLink =
                        accessInfo?.optString("webReaderLink", "")?.replace("http://", "https://")
                }

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(webReaderLink)
                }
            } catch (e: Exception) {
                Log.e("BookAdapter", "Ошибка загрузки книги: ${e.message}")
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(null)
                }
            }
        }
    }








}


