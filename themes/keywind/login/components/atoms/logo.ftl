<#macro kw>
  <div class="text-center">
    <img
      alt="Logo"
      class="mx-auto mb-2 h-12 dark:hidden"
      src="${url.resourcesPath}/img/minet_light.svg"
    />
    <img
      alt="Logo"
      class="mx-auto mb-2 h-12 hidden dark:block"
      src="${url.resourcesPath}/img/minet_dark.svg"
    />
    <div class="font-bold text-2xl">
      <#nested>
    </div>
  </div>
</#macro>
