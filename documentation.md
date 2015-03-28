---
layout: page
title: "Documentation"
---

Documentation

<div>
  {% for node in site.docs %}
    {% if node.title != null %}
      {% if node.layout == "page" %}
        <a class="sidebar-nav-item{% if page.url == node.url %} active{% endif %}" style="padding-left: 1em" href="{{ site.baseurl }}{{ node.url }}">{{ node.title }}</a>
      {% endif %}
    {% endif %}
  {% endfor %}
</div>
